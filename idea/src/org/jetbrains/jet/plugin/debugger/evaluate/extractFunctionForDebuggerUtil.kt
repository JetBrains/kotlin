/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.debugger.evaluate

import org.jetbrains.jet.lang.psi.JetCodeFragment
import com.intellij.psi.PsiFile
import org.jetbrains.jet.lang.psi.JetNamedFunction
import org.jetbrains.jet.plugin.refactoring.extractFunction.AnalysisResult
import org.jetbrains.jet.plugin.refactoring.extractFunction.AnalysisResult.ErrorMessage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.plugin.codeInsight.CodeInsightUtils
import org.jetbrains.jet.plugin.refactoring.createTempCopy
import org.jetbrains.jet.lang.psi.codeFragmentUtil.skipVisibilityCheck
import com.intellij.psi.PsiElement
import org.jetbrains.jet.plugin.refactoring.extractFunction.ExtractionData
import java.util.Collections
import org.jetbrains.jet.plugin.refactoring.extractFunction.performAnalysis
import org.jetbrains.jet.plugin.refactoring.extractFunction.AnalysisResult.Status
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil
import org.jetbrains.jet.plugin.refactoring.extractFunction.validate
import org.jetbrains.jet.plugin.refactoring.extractFunction.generateFunction
import org.jetbrains.jet.lang.psi.JetImportList
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.plugin.refactoring.extractFunction.ExtractionOptions

fun getFunctionForExtractedFragment(
        codeFragment: JetCodeFragment,
        breakpointFile: PsiFile,
        breakpointLine: Int
): JetNamedFunction? {

    fun getErrorMessageForExtractFunctionResult(analysisResult: AnalysisResult): String {
        return analysisResult.messages.map { errorMessage ->
            val message = when(errorMessage) {
                ErrorMessage.NO_EXPRESSION -> "Cannot perform an action without an expression"
                ErrorMessage.NO_CONTAINER -> "Cannot perform an action at this breakpoint ${breakpointFile.getName()}:${breakpointLine}"
                ErrorMessage.SUPER_CALL -> "Cannot perform an action for expression with super call"
                ErrorMessage.DENOTABLE_TYPES -> "Cannot perform an action because following types are unavailable from debugger scope"
                ErrorMessage.ERROR_TYPES -> "Cannot perform an action because this code fragment contains erroneous types"
                ErrorMessage.MULTIPLE_OUTPUT -> "Cannot perform an action because this code fragment changes more than one variable"
                ErrorMessage.DECLARATIONS_OUT_OF_SCOPE,
                ErrorMessage.OUTPUT_AND_EXIT_POINT,
                ErrorMessage.MULTIPLE_EXIT_POINTS,
                ErrorMessage.DECLARATIONS_ARE_USED_OUTSIDE -> "Cannot perform an action for this expression"
            }
            errorMessage.additionalInfo?.let { "$message: ${it.joinToString(", ")}" } ?: message
        }.joinToString(", ")
    }

    return ApplicationManager.getApplication()?.runReadAction(object: Computable<JetNamedFunction> {
        override fun compute(): JetNamedFunction? {
            checkForSyntacticErrors(codeFragment)

            val originalFile = breakpointFile as JetFile

            val lineStart = CodeInsightUtils.getStartLineOffset(originalFile, breakpointLine)
            if (lineStart == null) return null

            val tmpFile = originalFile.createTempCopy { it }
            tmpFile.skipVisibilityCheck = true

            val elementAtOffset = tmpFile.findElementAt(lineStart)
            if (elementAtOffset == null) return null

            val contextElement: PsiElement = CodeInsightUtils.getTopmostElementAtOffset(elementAtOffset, lineStart) ?: elementAtOffset

            addImportsToFile(codeFragment.importsAsImportList(), tmpFile)

            val newDebugExpression = addDebugExpressionBeforeContextElement(codeFragment, contextElement)
            if (newDebugExpression == null) return null

            val targetSibling = tmpFile.getDeclarations().firstOrNull()
            if (targetSibling == null) return null

            val analysisResult = ExtractionData(
                    tmpFile, Collections.singletonList(newDebugExpression), targetSibling, ExtractionOptions(false)
            ).performAnalysis()
            if (analysisResult.status != Status.SUCCESS) {
                throw EvaluateExceptionUtil.createEvaluateException(getErrorMessageForExtractFunctionResult(analysisResult))
            }

            val validationResult = analysisResult.descriptor!!.validate()
            if (!validationResult.conflicts.isEmpty()) {
                throw EvaluateExceptionUtil.createEvaluateException("Following declarations are unavailable in debug scope: ${validationResult.conflicts.keySet()?.map { it.getText() }?.makeString(",")}")
            }

            return validationResult.descriptor.generateFunction(true)
        }
    })
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

private fun addDebugExpressionBeforeContextElement(codeFragment: JetCodeFragment, contextElement: PsiElement): JetExpression? {
    val parent = contextElement.getParent()
    if (parent == null) return null

    val psiFactory = JetPsiFactory(codeFragment)
    parent.addBefore(psiFactory.createNewLine(), contextElement)

    val debugExpression = codeFragment.getContentElement()
    if (debugExpression == null) return null

    val newDebugExpression = parent.addBefore(debugExpression, contextElement)
    if (newDebugExpression == null) return null

    parent.addBefore(psiFactory.createNewLine(), contextElement)

    return newDebugExpression as JetExpression
}
