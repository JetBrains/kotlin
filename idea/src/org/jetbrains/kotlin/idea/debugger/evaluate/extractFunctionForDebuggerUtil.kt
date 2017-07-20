/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.debugger.evaluate

import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil
import com.intellij.diagnostic.LogMessageEx
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ExceptionUtil
import org.jetbrains.kotlin.idea.actions.internal.KotlinInternalMode
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.*
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.AnalysisResult.ErrorMessage
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.AnalysisResult.Status
import org.jetbrains.kotlin.idea.runInReadActionWithWriteActionPriorityWithPCE
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.attachment.attachmentByPsiFile
import org.jetbrains.kotlin.idea.util.attachment.mergeAttachments
import org.jetbrains.kotlin.idea.util.psi.patternMatching.toRange
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.codeFragmentUtil.suppressDiagnosticsInDebugMode
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext.SMARTCAST

fun getFunctionForExtractedFragment(
        codeFragment: KtCodeFragment,
        breakpointFile: PsiFile,
        breakpointLine: Int
): ExtractionResult? {

    fun getErrorMessageForExtractFunctionResult(analysisResult: AnalysisResult, tmpFile: KtFile): String {
        if (KotlinInternalMode.enabled) {
            val attachments = arrayOf(attachmentByPsiFile(tmpFile),
                                      attachmentByPsiFile(breakpointFile),
                                      attachmentByPsiFile(codeFragment),
                                      Attachment("breakpoint.info", "line: $breakpointLine"),
                                      Attachment("context.info", codeFragment.context?.text ?: "null"),
                                      Attachment("errors.info", analysisResult.messages.joinToString("\n") { "$it: ${it.renderMessage()}" }))
            LOG.error(LogMessageEx.createEvent(
                    "Internal error during evaluate expression",
                    ExceptionUtil.getThrowableText(Throwable("Extract function fails with ${analysisResult.messages.joinToString { it.name }}")),
                    mergeAttachments(*attachments)))
        }
        return analysisResult.messages.joinToString(", ") { errorMessage ->
            val message = when(errorMessage) {
                ErrorMessage.NO_EXPRESSION -> "Cannot perform an action without an expression"
                ErrorMessage.NO_CONTAINER -> "Cannot perform an action at this breakpoint ${breakpointFile.name}:$breakpointLine"
                ErrorMessage.SYNTAX_ERRORS -> "Cannot perform an action due to erroneous code"
                ErrorMessage.SUPER_CALL -> "Cannot perform an action for expression with super call"
                ErrorMessage.DENOTABLE_TYPES -> "Cannot perform an action because following types are unavailable from debugger scope"
                ErrorMessage.ERROR_TYPES -> "Cannot perform an action because this code fragment contains erroneous types"
                ErrorMessage.MULTIPLE_EXIT_POINTS,
                ErrorMessage.DECLARATIONS_OUT_OF_SCOPE,
                ErrorMessage.OUTPUT_AND_EXIT_POINT,
                ErrorMessage.DECLARATIONS_ARE_USED_OUTSIDE -> "Cannot perform an action for this expression"
                ErrorMessage.MULTIPLE_OUTPUT -> throw AssertionError("Unexpected error: $errorMessage")
            }
            errorMessage.additionalInfo?.let { "$message: ${it.joinToString(", ")}" } ?: message
        }
    }

    fun generateFunction(): ExtractionResult? {
        val originalFile = codeFragment.getContextContainingFile() ?: return null

        val newDebugExpressions = addDebugExpressionIntoTmpFileForExtractFunction(originalFile, codeFragment, breakpointLine)
        if (newDebugExpressions.isEmpty()) return null
        val tmpFile = newDebugExpressions.first().containingKtFile

        if (LOG.isDebugEnabled) {
            LOG.debug("TMP_FILE:\n${runReadAction { tmpFile.text }}")
        }

        val targetSibling = tmpFile.declarations.firstOrNull() ?: return null

        val options = ExtractionOptions(inferUnitTypeForUnusedValues = false,
                                        enableListBoxing = true,
                                        allowSpecialClassNames = true,
                                        captureLocalFunctions = true,
                                        canWrapInWith = true)
        val analysisResult = ExtractionData(tmpFile, newDebugExpressions.toRange(), targetSibling, null, options).performAnalysis()
        if (analysisResult.status != Status.SUCCESS) {
            throw EvaluateExceptionUtil.createEvaluateException(getErrorMessageForExtractFunctionResult(analysisResult, tmpFile))
        }

        val validationResult = analysisResult.descriptor!!.validate()
        if (!validationResult.conflicts.isEmpty) {
            throw EvaluateExceptionUtil.createEvaluateException("Following declarations are unavailable in debug scope: ${validationResult.conflicts.keySet().joinToString(",") { it.text }}")
        }

        val generatorOptions = ExtractionGeneratorOptions(inTempFile = true,
                                                          dummyName = GENERATED_FUNCTION_NAME,
                                                          allowExpressionBody = false)
        return ExtractionGeneratorConfiguration(validationResult.descriptor, generatorOptions).generateDeclaration()
    }

    return runReadAction { generateFunction() }
}

fun addDebugExpressionIntoTmpFileForExtractFunction(originalFile: KtFile, codeFragment: KtCodeFragment, line: Int): List<KtExpression> {
    codeFragment.markContextElement()
    codeFragment.markSmartCasts()

    val tmpFile = originalFile.copy() as KtFile
    tmpFile.suppressDiagnosticsInDebugMode = true
    tmpFile.analysisContext = originalFile.analysisContext

    val contextElement = getExpressionToAddDebugExpressionBefore(tmpFile, codeFragment.getOriginalContext(), line) ?: return emptyList()

    addImportsToFile(codeFragment.importsAsImportList(), tmpFile)

    val contentElementsInTmpFile = addDebugExpressionBeforeContextElement(codeFragment, contextElement)
    contentElementsInTmpFile.forEach { it.insertSmartCasts() }

    codeFragment.clearContextElement()
    codeFragment.clearSmartCasts()

    return contentElementsInTmpFile
}

private var PsiElement.IS_CONTEXT_ELEMENT: Boolean by NotNullableCopyableUserDataProperty(Key.create("IS_CONTEXT_ELEMENT"), false)

private fun KtCodeFragment.markContextElement() {
    getOriginalContext()?.IS_CONTEXT_ELEMENT = true
}

private fun KtCodeFragment.clearContextElement() {
    getOriginalContext()?.IS_CONTEXT_ELEMENT = false
}

private fun KtFile.findContextElement(): KtElement? {
    return this.findDescendantOfType { it.IS_CONTEXT_ELEMENT == true }
}

private var PsiElement.DEBUG_SMART_CAST: PsiElement? by CopyableUserDataProperty(Key.create("DEBUG_SMART_CAST"))

private fun KtCodeFragment.markSmartCasts() {
    val bindingContext = runInReadActionWithWriteActionPriorityWithPCE { analyzeFully() }
    val factory = KtPsiFactory(project)

    getContentElement()?.forEachDescendantOfType<KtExpression> { expression ->
        val smartCast = bindingContext.get(SMARTCAST, expression)?.defaultType
        if (smartCast != null) {
            val smartCastedExpression = factory.createExpressionByPattern(
                    "($0 as ${DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(smartCast)})",
                    expression) as KtParenthesizedExpression

            expression.DEBUG_SMART_CAST = smartCastedExpression
        }
    }
}

private fun KtExpression.insertSmartCasts() {
    forEachDescendantOfType<KtExpression> {
        val replacement = it.DEBUG_SMART_CAST
        if (replacement != null) runReadAction { it.replace(replacement) }
    }
}

private fun KtCodeFragment.clearSmartCasts() {
    getContentElement()?.forEachDescendantOfType<KtExpression> { it.DEBUG_SMART_CAST = null }
}

private fun addImportsToFile(newImportList: KtImportList?, tmpFile: KtFile) {
    if (newImportList != null && newImportList.imports.isNotEmpty()) {
        val tmpFileImportList = tmpFile.importList
        val psiFactory = KtPsiFactory(tmpFile)
        if (tmpFileImportList == null) {
            val packageDirective = tmpFile.packageDirective
            tmpFile.addAfter(psiFactory.createNewLine(), packageDirective)
            tmpFile.addAfter(newImportList, tmpFile.packageDirective)
        }
        else {
            newImportList.imports.forEach {
                tmpFileImportList.add(psiFactory.createNewLine())
                tmpFileImportList.add(it)
            }

            tmpFileImportList.add(psiFactory.createNewLine())
        }
    }
}

private fun getExpressionToAddDebugExpressionBefore(tmpFile: KtFile, contextElement: PsiElement?, line: Int): PsiElement? {
    if (contextElement == null) {
        val lineStart = CodeInsightUtils.getStartLineOffset(tmpFile, line) ?: return null

        val elementAtOffset = tmpFile.findElementAt(lineStart) ?: return null

        return CodeInsightUtils.getTopmostElementAtOffset(elementAtOffset, lineStart)
    }

    fun shouldStop(el: PsiElement?, p: PsiElement?) = p is KtBlockExpression || el is KtDeclaration || el is KtFile

    val elementAt = tmpFile.findContextElement()

    var parent = elementAt?.parent
    if (shouldStop(elementAt, parent)) {
        return elementAt
    }

    var parentOfParent = parent?.parent

    while (parent != null && parentOfParent != null) {
        if (shouldStop(parent, parentOfParent)) {
            break
        }

        parent = parent.parent
        parentOfParent = parent?.parent
    }

    return parent
}

private fun addDebugExpressionBeforeContextElement(codeFragment: KtCodeFragment, contextElement: PsiElement): List<KtExpression> {
    val elementBefore = findElementBefore(contextElement)

    val parent = elementBefore?.parent ?: return emptyList()

    val psiFactory = KtPsiFactory(codeFragment)

    parent.addBefore(psiFactory.createNewLine(), elementBefore)

    fun insertExpression(expr: KtElement?): List<KtExpression> {
        when (expr) {
            is KtBlockExpression -> return expr.statements.flatMap(::insertExpression)
            is KtExpression -> {
                val newDebugExpression = parent.addBefore(expr, elementBefore)
                if (newDebugExpression == null) {
                    LOG.error("Couldn't insert debug expression ${expr.text} to context file before ${elementBefore.text}")
                    return emptyList()
                }
                parent.addBefore(psiFactory.createNewLine(), elementBefore)
                return listOf(newDebugExpression as KtExpression)
            }
        }
        return emptyList()
    }

    val containingFile = codeFragment.context?.containingFile
    if (containingFile is KtCodeFragment) {
        insertExpression(containingFile.getContentElement() as? KtExpression)
    }

    val debugExpression = codeFragment.getContentElement() ?: return emptyList()
    return insertExpression(debugExpression)
}

private fun findElementBefore(contextElement: PsiElement): PsiElement? {
    val psiFactory = KtPsiFactory(contextElement)

    fun insertNewInitializer(classBody: KtClassBody): PsiElement? {
        val initializer = psiFactory.createAnonymousInitializer()
        val newInitializer = (classBody.addAfter(initializer, classBody.firstChild) as KtAnonymousInitializer)
        val block = newInitializer.body as KtBlockExpression?
        return block?.lastChild
    }

    return when {
        contextElement is KtFile -> {
            val fakeFunction = psiFactory.createFunction("fun _debug_fun_() {}")
            contextElement.add(psiFactory.createNewLine())
            val newFakeFun = contextElement.add(fakeFunction) as KtNamedFunction
            newFakeFun.bodyExpression!!.lastChild
        }
        contextElement is KtProperty && !contextElement.isLocal -> {
            val delegateExpressionOrInitializer = contextElement.delegateExpressionOrInitializer
            if (delegateExpressionOrInitializer != null) {
                wrapInLambdaCall(delegateExpressionOrInitializer)
            }
            else {
                val getter = contextElement.getter!!
                if (!getter.hasBlockBody()) {
                    wrapInLambdaCall(getter.bodyExpression!!)
                }
                else {
                    (getter.bodyExpression as KtBlockExpression).statements.first()
                }
            }
        }
        contextElement is KtParameter -> {
            val ownerFunction = contextElement.ownerFunction!!
            findElementBefore(ownerFunction)
        }
        contextElement is KtPrimaryConstructor -> {
            val classOrObject = contextElement.getContainingClassOrObject()
            insertNewInitializer(classOrObject.getOrCreateBody())
        }
        contextElement is KtClassOrObject -> {
            insertNewInitializer(contextElement.getOrCreateBody())
        }
        contextElement is KtFunctionLiteral -> {
            val block = contextElement.bodyExpression!!
            block.statements.firstOrNull() ?: block.lastChild
        }
        contextElement is KtDeclarationWithBody && !contextElement.hasBody() -> {
            val block = psiFactory.createBlock("")
            val newBlock = contextElement.add(block) as KtBlockExpression
            newBlock.rBrace
        }
        contextElement is KtDeclarationWithBody && !contextElement.hasBlockBody() -> {
            wrapInLambdaCall(contextElement.bodyExpression!!)
        }
        contextElement is KtDeclarationWithBody && contextElement.hasBlockBody() -> {
            val block = contextElement.bodyExpression as KtBlockExpression
            val last = block.statements.lastOrNull()
            last as? KtReturnExpression ?: block.rBrace
        }
        contextElement is KtWhenEntry -> {
            val entryExpression = contextElement.expression
            if (entryExpression is KtBlockExpression) {
                entryExpression.statements.firstOrNull() ?: entryExpression.lastChild
            }
            else {
                wrapInLambdaCall(entryExpression!!)
            }
        }
        else -> {
            contextElement
        }
    }
}

private fun replaceByLambdaCall(expression: KtExpression): KtCallExpression {
    val callExpression = KtPsiFactory(expression).createExpression("{ \n${expression.text} \n}()") as KtCallExpression
    return expression.replaced(callExpression)
}

private fun wrapInLambdaCall(expression: KtExpression): PsiElement? {
    val replacedBody = replaceByLambdaCall(expression)
    return (replacedBody.calleeExpression as? KtLambdaExpression)?.bodyExpression?.firstChild
}
