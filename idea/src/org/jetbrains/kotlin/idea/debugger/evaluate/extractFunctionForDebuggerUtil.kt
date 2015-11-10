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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiModificationTrackerImpl
import org.jetbrains.kotlin.idea.actions.internal.KotlinInternalMode
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.core.refactoring.createTempCopy
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.intentions.InsertExplicitTypeArgumentsIntention
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.*
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.AnalysisResult.ErrorMessage
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.AnalysisResult.Status
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.psi.patternMatching.toRange
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.codeFragmentUtil.suppressDiagnosticsInDebugMode

fun getFunctionForExtractedFragment(
        codeFragment: KtCodeFragment,
        breakpointFile: PsiFile,
        breakpointLine: Int
): ExtractionResult? {

    fun getErrorMessageForExtractFunctionResult(analysisResult: AnalysisResult, tmpFile: KtFile): String {
        if (KotlinInternalMode.enabled) {
            LOG.error("Couldn't extract function for debugger:\n" +
                      "FILE NAME: ${breakpointFile.name}\n" +
                      "BREAKPOINT LINE: $breakpointLine\n" +
                      "CODE FRAGMENT:\n${codeFragment.text}\n" +
                      "ERRORS:\n${analysisResult.messages.map { "$it: ${it.renderMessage()}" }.joinToString("\n")}\n" +
                      "TMPFILE_TEXT:\n${tmpFile.text}\n" +
                      "FILE TEXT: \n${breakpointFile.text}\n")
        }
        return analysisResult.messages.map { errorMessage ->
            val message = when(errorMessage) {
                ErrorMessage.NO_EXPRESSION -> "Cannot perform an action without an expression"
                ErrorMessage.NO_CONTAINER -> "Cannot perform an action at this breakpoint ${breakpointFile.name}:$breakpointLine"
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
        }.joinToString(", ")
    }

    fun generateFunction(): ExtractionResult? {
        val originalFile = breakpointFile as KtFile

        val newDebugExpressions = addDebugExpressionIntoTmpFileForExtractFunction(originalFile, codeFragment, breakpointLine)
        if (newDebugExpressions.isEmpty()) return null
        val tmpFile = newDebugExpressions.first().getContainingKtFile()

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
            throw EvaluateExceptionUtil.createEvaluateException("Following declarations are unavailable in debug scope: ${validationResult.conflicts.keySet().map { it.text }.joinToString(",")}")
        }

        val generatorOptions = ExtractionGeneratorOptions(inTempFile = true,
                                                          flexibleTypesAllowed = true,
                                                          allowDummyName = true,
                                                          allowExpressionBody = false)
        return ExtractionGeneratorConfiguration(validationResult.descriptor, generatorOptions).generateDeclaration()
    }

    return runReadAction { generateFunction() }
}

fun addDebugExpressionIntoTmpFileForExtractFunction(originalFile: KtFile, codeFragment: KtCodeFragment, line: Int): List<KtExpression> {
    val tmpFile = originalFile.createTempCopy { it }
    tmpFile.suppressDiagnosticsInDebugMode = true

    val contextElement = getExpressionToAddDebugExpressionBefore(tmpFile, codeFragment.context, line) ?: return emptyList()

    addImportsToFile(codeFragment.importsAsImportList(), tmpFile)

    return addDebugExpressionBeforeContextElement(codeFragment, contextElement)
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

private fun KtFile.getElementInCopy(e: PsiElement): PsiElement? {
    val offset = e.textRange?.startOffset ?: return null
    var elementAt = this.findElementAt(offset)
    while (elementAt == null || elementAt.textRange?.endOffset != e.textRange?.endOffset) {
        elementAt = elementAt?.parent
    }
    return elementAt
}

private fun getExpressionToAddDebugExpressionBefore(tmpFile: KtFile, contextElement: PsiElement?, line: Int): PsiElement? {
    if (contextElement == null) {
        val lineStart = CodeInsightUtils.getStartLineOffset(tmpFile, line) ?: return null

        val elementAtOffset = tmpFile.findElementAt(lineStart) ?: return null

        return CodeInsightUtils.getTopmostElementAtOffset(elementAtOffset, lineStart) ?: elementAtOffset
    }

    val containingFile = contextElement.containingFile
    if (containingFile is KtCodeFragment) {
        return getExpressionToAddDebugExpressionBefore(tmpFile, containingFile.context, line)
    }

    fun shouldStop(el: PsiElement?, p: PsiElement?) = p is KtBlockExpression || el is KtDeclaration || el is KtFile

    var elementAt = tmpFile.getElementInCopy(contextElement)

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
    val psiFactory = KtPsiFactory(codeFragment)

    fun insertNewInitializer(classBody: KtClassBody): PsiElement? {
        val initializer = psiFactory.createAnonymousInitializer()
        val newInitializer = (classBody.addAfter(initializer, classBody.firstChild) as KtClassInitializer)
        val block = newInitializer.body as KtBlockExpression?
        return block?.lastChild
    }

    val elementBefore = when {
        contextElement is KtFile -> {
            val fakeFunction = psiFactory.createFunction("fun _debug_fun_() {}")
            contextElement.add(psiFactory.createNewLine())
            val newFakeFun = contextElement.add(fakeFunction) as KtNamedFunction
            newFakeFun.bodyExpression!!.lastChild
        }
        contextElement is KtProperty && !contextElement.isLocal -> {
            val delegateExpressionOrInitializer = contextElement.delegateExpressionOrInitializer
            if (delegateExpressionOrInitializer != null) {
                wrapInRunFun(delegateExpressionOrInitializer)
            }
            else {
                val getter = contextElement.getter!!
                if (!getter.hasBlockBody()) {
                    wrapInRunFun(getter.bodyExpression!!)
                }
                else {
                    (getter.bodyExpression as KtBlockExpression).statements.first()
                }
            }
        }
        contextElement is KtPrimaryConstructor -> {
            val classOrObject = contextElement.getContainingClassOrObject()
            insertNewInitializer(classOrObject.getOrCreateBody())
        }
        contextElement is KtClassOrObject -> {
            insertNewInitializer(contextElement.getBody()!!)
        }
        contextElement is KtFunctionLiteral -> {
            val block = contextElement.bodyExpression!!
            block.statements.firstOrNull() ?: block.lastChild
        }
        contextElement is KtDeclarationWithBody && !contextElement.hasBody()-> {
            val block = psiFactory.createBlock("")
            val newBlock = contextElement.add(block) as KtBlockExpression
            newBlock.rBrace
        }
        contextElement is KtDeclarationWithBody && !contextElement.hasBlockBody()-> {
            wrapInRunFun(contextElement.bodyExpression!!)
        }
        contextElement is KtDeclarationWithBody && contextElement.hasBlockBody()-> {
            val block = contextElement.bodyExpression as KtBlockExpression
            val last = block.statements.lastOrNull()
            if (last is KtReturnExpression)
                last
            else
                block.rBrace
        }
        contextElement is KtWhenEntry -> {
            val entryExpression = contextElement.expression
            if (entryExpression is KtBlockExpression) {
                entryExpression.statements.firstOrNull() ?: entryExpression.lastChild
            }
            else {
                wrapInRunFun(entryExpression!!)
            }
        }
        else -> {
            contextElement
        }
    }

    val parent = elementBefore?.parent
    if (parent == null || elementBefore == null) return emptyList()

    parent.addBefore(psiFactory.createNewLine(), elementBefore)

    fun insertExpression(expr: KtElement?): List<KtExpression> {
        when (expr) {
            is KtBlockExpression -> return expr.statements.flatMap { insertExpression(it) }
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

private fun replaceByRunFunction(expression: KtExpression): KtCallExpression {
    val callExpression = KtPsiFactory(expression).createExpression("run { \n${expression.text} \n}") as KtCallExpression
    val replaced = expression.replaced(callExpression)
    val typeArguments = InsertExplicitTypeArgumentsIntention.createTypeArguments(replaced, replaced.analyze())
    if (typeArguments?.arguments?.isNotEmpty() ?: false) {
        val calleeExpression = replaced.calleeExpression
        replaced.addAfter(typeArguments!!, calleeExpression)
    }
    return replaced
}

private fun wrapInRunFun(expression: KtExpression): PsiElement? {
    val replacedBody = replaceByRunFunction(expression)

    // Increment modification tracker to clear ResolveCache after changes in function body
    (PsiManager.getInstance(expression.project).modificationTracker as PsiModificationTrackerImpl).incCounter()

    return replacedBody.functionLiteralArguments.first().getFunctionLiteral().bodyExpression?.firstChild
}
