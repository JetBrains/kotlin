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

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.AnalysisResult.ErrorMessage
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.refactoring.createTempCopy
import org.jetbrains.kotlin.psi.codeFragmentUtil.suppressDiagnosticsInDebugMode
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.AnalysisResult.Status
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil
import org.jetbrains.kotlin.idea.util.application.runReadAction
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiModificationTrackerImpl
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.idea.intentions.InsertExplicitTypeArguments
import org.jetbrains.kotlin.idea.util.psi.patternMatching.toRange
import org.jetbrains.kotlin.idea.actions.internal.KotlinInternalMode
import org.jetbrains.kotlin.psi.psiUtil.replaced
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.*

fun getFunctionForExtractedFragment(
        codeFragment: JetCodeFragment,
        breakpointFile: PsiFile,
        breakpointLine: Int
): ExtractionResult? {

    fun getErrorMessageForExtractFunctionResult(analysisResult: AnalysisResult): String {
        if (KotlinInternalMode.enabled) {
            logger.error("Couldn't extract function for debugger:\n" +
                                 "FILE NAME: ${breakpointFile.getName()}\n" +
                                 "BREAKPOINT LINE: ${breakpointLine}\n" +
                                 "CODE FRAGMENT:\n${codeFragment.getText()}\n" +
                                 "ERRORS:\n${analysisResult.messages.map { "$it: ${it.renderMessage()}" }.joinToString("\n")}\n" +
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
        val originalFile = breakpointFile as JetFile

        val tmpFile = originalFile.createTempCopy { it }
        tmpFile.suppressDiagnosticsInDebugMode = true

        val contextElement = getExpressionToAddDebugExpressionBefore(tmpFile, codeFragment.getContext(), breakpointLine)
        if (contextElement == null) return null

        // Don't evaluate smth when breakpoint is on package directive (ex. for package classes)
        if (contextElement is JetFile) {
            throw EvaluateExceptionUtil.createEvaluateException("Cannot perform an action at this breakpoint ${breakpointFile.getName()}:${breakpointLine}")
        }

        addImportsToFile(codeFragment.importsAsImportList(), tmpFile)

        val newDebugExpression = addDebugExpressionBeforeContextElement(codeFragment, contextElement)
        if (newDebugExpression == null) return null

        val targetSibling = tmpFile.getDeclarations().firstOrNull()
        if (targetSibling == null) return null

        val options = ExtractionOptions(inferUnitTypeForUnusedValues = false, 
                                        enableListBoxing = true,
                                        allowSpecialClassNames = true)
        val analysisResult = ExtractionData(tmpFile, newDebugExpression.toRange(), targetSibling, options).performAnalysis()
        if (analysisResult.status != Status.SUCCESS) {
            throw EvaluateExceptionUtil.createEvaluateException(getErrorMessageForExtractFunctionResult(analysisResult))
        }

        val validationResult = analysisResult.descriptor!!.validate()
        if (!validationResult.conflicts.isEmpty()) {
            throw EvaluateExceptionUtil.createEvaluateException("Following declarations are unavailable in debug scope: ${validationResult.conflicts.keySet().map { it.getText() }.joinToString(",")}")
        }

        val config = ExtractionGeneratorConfiguration(
                validationResult.descriptor,
                ExtractionGeneratorOptions(inTempFile = true, flexibleTypesAllowed = true)
        )
        return config.generateDeclaration()
    }

    return runReadAction { generateFunction() }
}

private fun addImportsToFile(newImportList: JetImportList?, tmpFile: JetFile) {
    if (newImportList != null) {
        val tmpFileImportList = tmpFile.getImportList()
        val packageDirective = tmpFile.getPackageDirective()
        val psiFactory = JetPsiFactory(tmpFile)
        if (tmpFileImportList == null) {
            tmpFile.addAfter(psiFactory.createNewLine(), packageDirective)
            tmpFile.addAfter(newImportList, tmpFile.getPackageDirective())
        }
        else {
            tmpFileImportList.replace(newImportList)
        }
        tmpFile.addAfter(psiFactory.createNewLine(), packageDirective)
    }
}

private fun JetFile.getElementInCopy(e: PsiElement): PsiElement? {
    val offset = e.getTextRange()?.getStartOffset()
    if (offset == null) {
        return null
    }
    var elementAt = this.findElementAt(offset)
    while (elementAt == null || elementAt!!.getTextRange()?.getEndOffset() != e.getTextRange()?.getEndOffset()) {
        elementAt = elementAt?.getParent()
    }
    return elementAt
}

private fun getExpressionToAddDebugExpressionBefore(tmpFile: JetFile, contextElement: PsiElement?, line: Int): PsiElement? {
    if (contextElement == null) {
        val lineStart = CodeInsightUtils.getStartLineOffset(tmpFile, line)
        if (lineStart == null) return null

        val elementAtOffset = tmpFile.findElementAt(lineStart)
        if (elementAtOffset == null) return null

        return CodeInsightUtils.getTopmostElementAtOffset(elementAtOffset, lineStart) ?: elementAtOffset
    }

    fun shouldStop(el: PsiElement?, p: PsiElement?) = p is JetBlockExpression || el is JetDeclaration

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

        parent = parent?.getParent()
        parentOfParent = parent?.getParent()
    }

    return parent
}

private fun addDebugExpressionBeforeContextElement(codeFragment: JetCodeFragment, contextElement: PsiElement): JetExpression? {
    val psiFactory = JetPsiFactory(codeFragment)

    fun insertNewInitializer(classBody: JetClassBody): PsiElement? {
        val initializer = psiFactory.createAnonymousInitializer()
        val newInitializer = (classBody.addAfter(initializer, classBody.getFirstChild()) as JetClassInitializer)
        val block = newInitializer.getBody() as JetBlockExpression
        return block.getLastChild()
    }

    val elementBefore = when {
        contextElement is JetProperty && !contextElement.isLocal() -> {
            wrapInRunFun(contextElement.getDelegateExpressionOrInitializer()!!)
        }
        contextElement is JetClassOrObject -> {
            insertNewInitializer(contextElement.getBody())
        }
        contextElement is JetFunctionLiteral -> {
            val block = contextElement.getBodyExpression()!!
            block.getStatements().firstOrNull() ?: block.getLastChild()
        }
        contextElement is JetDeclarationWithBody && !contextElement.hasBlockBody()-> {
            wrapInRunFun(contextElement.getBodyExpression()!!)
        }
        contextElement is JetDeclarationWithBody && contextElement.hasBlockBody()-> {
            val block = contextElement.getBodyExpression() as JetBlockExpression
            val last = block.getStatements().lastOrNull()
            if (last is JetReturnExpression)
                last
            else
                block.getRBrace()
        }
        contextElement is JetWhenEntry -> {
            val entryExpression = contextElement.getExpression()
            if (entryExpression is JetBlockExpression) {
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
    if (parent == null || elementBefore == null) return null

    parent.addBefore(psiFactory.createNewLine(), elementBefore)

    val debugExpression = codeFragment.getContentElement()
    if (debugExpression == null) return null

    val newDebugExpression = parent.addBefore(debugExpression, elementBefore)
    if (newDebugExpression == null) return null

    parent.addBefore(psiFactory.createNewLine(), elementBefore)

    return newDebugExpression as JetExpression
}

private fun replaceByRunFunction(expression: JetExpression): JetCallExpression {
    val callExpression = JetPsiFactory(expression).createExpression("run { \n${expression.getText()} \n}") as JetCallExpression
    val replaced = expression.replaced(callExpression)
    val typeArguments = InsertExplicitTypeArguments.createTypeArguments(replaced)
    if (typeArguments?.getArguments()?.isNotEmpty() ?: false) {
        val calleeExpression = replaced.getCalleeExpression()
        replaced.addAfter(typeArguments!!, calleeExpression)
    }
    return replaced
}

private fun wrapInRunFun(expression: JetExpression): PsiElement? {
    val replacedBody = replaceByRunFunction(expression)

    // Increment modification tracker to clear ResolveCache after changes in function body
    (PsiManager.getInstance(expression.getProject()).getModificationTracker() as PsiModificationTrackerImpl).incCounter()

    return replacedBody.getFunctionLiteralArguments().first().getFunctionLiteral().getBodyExpression()?.getFirstChild()
}
