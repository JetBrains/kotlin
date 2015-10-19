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
            logger.error("Couldn't extract function for debugger:\n" +
                                 "FILE NAME: ${breakpointFile.getName()}\n" +
                                 "BREAKPOINT LINE: ${breakpointLine}\n" +
                                 "CODE FRAGMENT:\n${codeFragment.getText()}\n" +
                                 "ERRORS:\n${analysisResult.messages.map { "$it: ${it.renderMessage()}" }.joinToString("\n")}\n" +
                                 "TMPFILE_TEXT:\n${tmpFile.text}\n" +
                                 "FILE TEXT: \n${breakpointFile.getText()}\n")
        }
        return analysisResult.messages.map { errorMessage ->
            val message = when(errorMessage) {
                ErrorMessage.NO_EXPRESSION -> "Cannot perform an action without an expression"
                ErrorMessage.NO_CONTAINER -> "Cannot perform an action at this breakpoint ${breakpointFile.getName()}:${breakpointLine}"
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

        val tmpFile = originalFile.createTempCopy { it }
        tmpFile.suppressDiagnosticsInDebugMode = true

        val contextElement = getExpressionToAddDebugExpressionBefore(tmpFile, codeFragment.getContext(), breakpointLine)
        if (contextElement == null) return null

        addImportsToFile(codeFragment.importsAsImportList(), tmpFile)

        val newDebugExpressions = addDebugExpressionBeforeContextElement(codeFragment, contextElement)
        if (newDebugExpressions.isEmpty()) return null

        val targetSibling = tmpFile.getDeclarations().firstOrNull()
        if (targetSibling == null) return null

        val options = ExtractionOptions(inferUnitTypeForUnusedValues = false,
                                        enableListBoxing = true,
                                        allowSpecialClassNames = true,
                                        captureLocalFunctions = true)
        val analysisResult = ExtractionData(tmpFile, newDebugExpressions.toRange(), targetSibling, null, options).performAnalysis()
        if (analysisResult.status != Status.SUCCESS) {
            throw EvaluateExceptionUtil.createEvaluateException(getErrorMessageForExtractFunctionResult(analysisResult, tmpFile))
        }

        val validationResult = analysisResult.descriptor!!.validate()
        if (!validationResult.conflicts.isEmpty()) {
            throw EvaluateExceptionUtil.createEvaluateException("Following declarations are unavailable in debug scope: ${validationResult.conflicts.keySet().map { it.getText() }.joinToString(",")}")
        }

        val generatorOptions = ExtractionGeneratorOptions(inTempFile = true,
                                                          flexibleTypesAllowed = true,
                                                          allowDummyName = true,
                                                          allowExpressionBody = false)
        return ExtractionGeneratorConfiguration(validationResult.descriptor, generatorOptions).generateDeclaration()
    }

    return runReadAction { generateFunction() }
}

private fun addImportsToFile(newImportList: KtImportList?, tmpFile: KtFile) {
    if (newImportList != null) {
        val tmpFileImportList = tmpFile.getImportList()
        val packageDirective = tmpFile.getPackageDirective()
        val psiFactory = KtPsiFactory(tmpFile)
        if (tmpFileImportList == null) {
            tmpFile.addAfter(psiFactory.createNewLine(), packageDirective)
            tmpFile.addAfter(newImportList, tmpFile.getPackageDirective())
        }
        else {
            val tmpFileImports = tmpFileImportList.imports
            if (tmpFileImports.isEmpty()) {
                tmpFileImportList.replace(newImportList)
            }
            else {
                val lastImport = tmpFileImports.last()
                newImportList.imports.forEach {
                    tmpFileImportList.addAfter(it, lastImport)
                }
                tmpFileImportList.addAfter(psiFactory.createNewLine(), lastImport)
            }
        }
        tmpFile.addAfter(psiFactory.createNewLine(), packageDirective)
    }
}

private fun KtFile.getElementInCopy(e: PsiElement): PsiElement? {
    val offset = e.getTextRange()?.getStartOffset()
    if (offset == null) {
        return null
    }
    var elementAt = this.findElementAt(offset)
    while (elementAt == null || elementAt.getTextRange()?.getEndOffset() != e.getTextRange()?.getEndOffset()) {
        elementAt = elementAt?.getParent()
    }
    return elementAt
}

private fun getExpressionToAddDebugExpressionBefore(tmpFile: KtFile, contextElement: PsiElement?, line: Int): PsiElement? {
    if (contextElement == null) {
        val lineStart = CodeInsightUtils.getStartLineOffset(tmpFile, line)
        if (lineStart == null) return null

        val elementAtOffset = tmpFile.findElementAt(lineStart)
        if (elementAtOffset == null) return null

        return CodeInsightUtils.getTopmostElementAtOffset(elementAtOffset, lineStart) ?: elementAtOffset
    }

    val containingFile = contextElement.getContainingFile()
    if (containingFile is KtCodeFragment) {
        return getExpressionToAddDebugExpressionBefore(tmpFile, containingFile.getContext(), line)
    }

    fun shouldStop(el: PsiElement?, p: PsiElement?) = p is KtBlockExpression || el is KtDeclaration || el is KtFile

    var elementAt = tmpFile.getElementInCopy(contextElement)

    var parent = elementAt?.getParent()
    if (shouldStop(elementAt, parent)) {
        return elementAt
    }

    var parentOfParent = parent?.getParent()

    while (parent != null && parentOfParent != null) {
        if (shouldStop(parent, parentOfParent)) {
            break
        }

        parent = parent.getParent()
        parentOfParent = parent?.getParent()
    }

    return parent
}

private fun addDebugExpressionBeforeContextElement(codeFragment: KtCodeFragment, contextElement: PsiElement): List<KtExpression> {
    val psiFactory = KtPsiFactory(codeFragment)

    fun insertNewInitializer(classBody: KtClassBody): PsiElement? {
        val initializer = psiFactory.createAnonymousInitializer()
        val newInitializer = (classBody.addAfter(initializer, classBody.getFirstChild()) as KtClassInitializer)
        val block = newInitializer.getBody() as KtBlockExpression?
        return block?.getLastChild()
    }

    val elementBefore = when {
        contextElement is KtFile -> {
            val fakeFunction = psiFactory.createFunction("fun _debug_fun_() {}")
            contextElement.add(psiFactory.createNewLine())
            val newFakeFun = contextElement.add(fakeFunction) as KtNamedFunction
            newFakeFun.getBodyExpression()!!.getLastChild()
        }
        contextElement is KtProperty && !contextElement.isLocal() -> {
            val delegateExpressionOrInitializer = contextElement.getDelegateExpressionOrInitializer()
            if (delegateExpressionOrInitializer != null) {
                wrapInRunFun(delegateExpressionOrInitializer)
            }
            else {
                val getter = contextElement.getGetter()!!
                if (!getter.hasBlockBody()) {
                    wrapInRunFun(getter.getBodyExpression()!!)
                }
                else {
                    (getter.getBodyExpression() as KtBlockExpression).getStatements().first()
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
            val block = contextElement.getBodyExpression()!!
            block.getStatements().firstOrNull() ?: block.getLastChild()
        }
        contextElement is KtDeclarationWithBody && !contextElement.hasBody()-> {
            val block = psiFactory.createBlock("")
            val newBlock = contextElement.add(block) as KtBlockExpression
            newBlock.getRBrace()
        }
        contextElement is KtDeclarationWithBody && !contextElement.hasBlockBody()-> {
            wrapInRunFun(contextElement.getBodyExpression()!!)
        }
        contextElement is KtDeclarationWithBody && contextElement.hasBlockBody()-> {
            val block = contextElement.getBodyExpression() as KtBlockExpression
            val last = block.getStatements().lastOrNull()
            if (last is KtReturnExpression)
                last
            else
                block.getRBrace()
        }
        contextElement is KtWhenEntry -> {
            val entryExpression = contextElement.getExpression()
            if (entryExpression is KtBlockExpression) {
                entryExpression.getStatements().firstOrNull() ?: entryExpression.getLastChild()
            }
            else {
                wrapInRunFun(entryExpression!!)
            }
        }
        else -> {
            contextElement
        }
    }

    val parent = elementBefore?.getParent()
    if (parent == null || elementBefore == null) return emptyList()

    parent.addBefore(psiFactory.createNewLine(), elementBefore)

    fun insertExpression(expr: KtElement?): List<KtExpression> {
        when (expr) {
            is KtBlockExpression -> return expr.getStatements().flatMap { insertExpression(it) }
            is KtExpression -> {
                val newDebugExpression = parent.addBefore(expr, elementBefore)
                if (newDebugExpression == null) {
                    logger.error("Couldn't insert debug expression ${expr.getText()} to context file before ${elementBefore.getText()}")
                    return emptyList()
                }
                parent.addBefore(psiFactory.createNewLine(), elementBefore)
                return listOf(newDebugExpression as KtExpression)
            }
        }
        return emptyList()
    }

    val containingFile = codeFragment.getContext()?.getContainingFile()
    if (containingFile is KtCodeFragment) {
        insertExpression(containingFile.getContentElement() as? KtExpression)
    }

    val debugExpression = codeFragment.getContentElement() ?: return emptyList()
    return insertExpression(debugExpression)
}

private fun replaceByRunFunction(expression: KtExpression): KtCallExpression {
    val callExpression = KtPsiFactory(expression).createExpression("run { \n${expression.getText()} \n}") as KtCallExpression
    val replaced = expression.replaced(callExpression)
    val typeArguments = InsertExplicitTypeArgumentsIntention.createTypeArguments(replaced, replaced.analyze())
    if (typeArguments?.getArguments()?.isNotEmpty() ?: false) {
        val calleeExpression = replaced.getCalleeExpression()
        replaced.addAfter(typeArguments!!, calleeExpression)
    }
    return replaced
}

private fun wrapInRunFun(expression: KtExpression): PsiElement? {
    val replacedBody = replaceByRunFunction(expression)

    // Increment modification tracker to clear ResolveCache after changes in function body
    (PsiManager.getInstance(expression.getProject()).getModificationTracker() as PsiModificationTrackerImpl).incCounter()

    return replacedBody.getFunctionLiteralArguments().first().getFunctionLiteral().getBodyExpression()?.getFirstChild()
}
