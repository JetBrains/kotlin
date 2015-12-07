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

package org.jetbrains.kotlin.idea.refactoring.inline

import com.google.common.collect.Sets
import com.intellij.codeInsight.highlighting.HighlightManager
import com.intellij.lang.Language
import com.intellij.lang.refactoring.InlineActionHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.HelpID
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.refactoring.util.RefactoringMessageDialog
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getCallWithAssert
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.utils.sure

class KotlinInlineValHandler : InlineActionHandler() {
    override fun isEnabledForLanguage(l: Language) = l == KotlinLanguage.INSTANCE

    override fun canInlineElement(element: PsiElement): Boolean {
        if (element !is KtProperty) return false
        return element.getter == null && element.receiverTypeReference == null
    }

    override fun inlineElement(project: Project, editor: Editor, element: PsiElement) {
        val declaration = element as KtProperty
        val file = declaration.getContainingKtFile()
        val name = declaration.name ?: return

        val referenceExpressions = ReferencesSearch.search(declaration).mapNotNull() {
            (it.element as? KtExpression)?.getQualifiedExpressionForSelectorOrThis()
        }

        if (referenceExpressions.isEmpty()) {
            val kind = if (declaration.isLocal) "Variable" else "Property"
            return showErrorHint(project, editor, "$kind '$name' is never used")
        }

        val assignments = Sets.newHashSet<PsiElement>()
        referenceExpressions.forEach { expression ->
            val parent = expression.parent

            val assignment = expression.getAssignmentByLHS()
            if (assignment != null) {
                assignments.add(parent)
            }

            if (parent is KtUnaryExpression && OperatorConventions.INCREMENT_OPERATIONS.contains(parent.operationToken)) {
                assignments.add(parent)
            }
        }

        val initializerInDeclaration = declaration.initializer
        val initializer = if (initializerInDeclaration != null) {
            if (!assignments.isEmpty()) return reportAmbiguousAssignment(project, editor, name, assignments)
            initializerInDeclaration
        }
        else {
            (assignments.singleOrNull() as? KtBinaryExpression)?.right
            ?: return reportAmbiguousAssignment(project, editor, name, assignments)
        }

        val typeArgumentsForCall = getTypeArgumentsStringForCall(initializer)
        val parametersForFunctionLiteral = getParametersForFunctionLiteral(initializer)

        val canHighlight = referenceExpressions.all { it.containingFile === file }
        if (canHighlight) {
            highlightExpressions(project, editor, referenceExpressions)
        }

        if (!showDialog(project, name, declaration, referenceExpressions)) {
            if (canHighlight) {
                val statusBar = WindowManager.getInstance().getStatusBar(project)
                statusBar?.info = RefactoringBundle.message("press.escape.to.remove.the.highlighting")
            }
            return
        }

        project.executeWriteCommand(RefactoringBundle.message("inline.command", name)) {
            val inlinedExpressions = referenceExpressions.mapNotNull { referenceExpression ->
                if (assignments.contains(referenceExpression.parent)) return@mapNotNull null
                referenceExpression.replaced(initializer)
            }

            assignments.forEach { it.delete() }
            declaration.delete()

            if (inlinedExpressions.isEmpty()) return@executeWriteCommand

            typeArgumentsForCall?.let { addTypeArguments(it, inlinedExpressions) }

            parametersForFunctionLiteral?.let { addFunctionLiteralParameterTypes(it, inlinedExpressions) }

            if (canHighlight) {
                highlightExpressions(project, editor, inlinedExpressions)
            }
        }
    }

    private fun reportAmbiguousAssignment(project: Project, editor: Editor, name: String, assignments: Set<PsiElement>) {
        val key = if (assignments.isEmpty()) "variable.has.no.initializer" else "variable.has.no.dominating.definition"
        val message = RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message(key, name))
        showErrorHint(project, editor, message)
    }

    private fun showErrorHint(project: Project, editor: Editor, message: String) {
        CommonRefactoringUtil.showErrorHint(project, editor, message, RefactoringBundle.message("inline.variable.title"), HelpID.INLINE_VARIABLE)
    }

    private fun highlightExpressions(project: Project, editor: Editor?, elements: List<PsiElement>) {
        if (editor == null || ApplicationManager.getApplication().isUnitTestMode) return

        val editorColorsManager = EditorColorsManager.getInstance()
        val searchResultsAttributes = editorColorsManager.globalScheme.getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES)
        val highlightManager = HighlightManager.getInstance(project)
        highlightManager.addOccurrenceHighlights(editor, elements.toTypedArray(), searchResultsAttributes, true, null)
    }

    private fun showDialog(
            project: Project,
            name: String,
            property: KtProperty,
            referenceExpressions: List<KtExpression>
    ): Boolean {
        if (ApplicationManager.getApplication().isUnitTestMode) return true

        val kind = if (property.isLocal) "local variable" else "property"
        val dialog = RefactoringMessageDialog(
                RefactoringBundle.message("inline.variable.title"),
                "Inline " + kind + " '" + name + "'? " + RefactoringBundle.message("occurences.string", referenceExpressions.size),
                HelpID.INLINE_VARIABLE,
                "OptionPane.questionIcon",
                true,
                project
        )
        dialog.show()
        return dialog.isOK
    }

    private fun getParametersForFunctionLiteral(initializer: KtExpression): String? {
        val functionLiteralExpression = initializer.unpackFunctionLiteral(true) ?: return null
        val context = initializer.analyze(BodyResolveMode.PARTIAL)
        val lambdaDescriptor = context.get(BindingContext.FUNCTION, functionLiteralExpression.functionLiteral)
        if (lambdaDescriptor == null || ErrorUtils.containsErrorType(lambdaDescriptor)) return null
        return lambdaDescriptor.valueParameters.joinToString {
            it.name.asString() + ": " + IdeDescriptorRenderers.SOURCE_CODE.renderType(it.type)
        }
    }

    private fun addFunctionLiteralParameterTypes(parameters: String, inlinedExpressions: List<KtExpression>) {
        val containingFile = inlinedExpressions.first().getContainingKtFile()
        val resolutionFacade = containingFile.getResolutionFacade()

        val functionsToAddParameters = inlinedExpressions.mapNotNull {
            val lambdaExpr = it.unpackFunctionLiteral(true).sure { "can't find function literal expression for " + it.text }
            if (needToAddParameterTypes(lambdaExpr, resolutionFacade)) lambdaExpr else null
        }

        val psiFactory = KtPsiFactory(containingFile)
        for (lambdaExpr in functionsToAddParameters) {
            val lambda = lambdaExpr.functionLiteral

            val currentParameterList = lambda.valueParameterList
            val newParameterList = psiFactory.createParameterList("($parameters)")
            if (currentParameterList != null) {
                currentParameterList.replace(newParameterList)
            }
            else {
                // TODO: Ugly code, need refactoring
                val openBraceElement = lambda.lBrace

                val nextSibling = openBraceElement.nextSibling
                val whitespaceToAdd = if (nextSibling is PsiWhiteSpace && nextSibling.text.contains("\n"))
                    nextSibling.copy()
                else
                    null

                val whitespaceAndArrow = psiFactory.createWhitespaceAndArrow()
                lambda.addRangeAfter(whitespaceAndArrow.first, whitespaceAndArrow.second, openBraceElement)

                lambda.addAfter(newParameterList, openBraceElement)
                if (whitespaceToAdd != null) {
                    lambda.addAfter(whitespaceToAdd, openBraceElement)
                }
            }
            ShortenReferences.DEFAULT.process(lambdaExpr.valueParameters)
        }
    }

    private fun needToAddParameterTypes(
            functionLiteralExpression: KtFunctionLiteralExpression,
            resolutionFacade: ResolutionFacade
    ): Boolean {
        val functionLiteral = functionLiteralExpression.functionLiteral
        val context = resolutionFacade.analyze(functionLiteralExpression, BodyResolveMode.PARTIAL)
        return context.diagnostics.any { diagnostic ->
            val factory = diagnostic.factory
            val element = diagnostic.psiElement
            val hasCantInferParameter = factory == Errors.CANNOT_INFER_PARAMETER_TYPE &&
                                        element.parent.parent == functionLiteral
            val hasUnresolvedItOrThis = factory == Errors.UNRESOLVED_REFERENCE &&
                                        element.text == "it" &&
                                        element.getStrictParentOfType<KtFunctionLiteral>() == functionLiteral
            hasCantInferParameter || hasUnresolvedItOrThis
        }
    }

    private fun addTypeArguments(typeArguments: String, inlinedExpressions: List<KtExpression>) {
        val containingFile = inlinedExpressions.first().getContainingKtFile()
        val callsToAddArguments = inlinedExpressions.mapNotNull {
            val context = it.analyze(BodyResolveMode.PARTIAL)
            val call = it.getCallWithAssert(context)
            val callElement = call.callElement
            if (callElement is KtCallExpression &&
                hasIncompleteTypeInferenceDiagnostic(call, context) &&
                call.typeArgumentList == null) callElement else null
        }

        val psiFactory = KtPsiFactory(containingFile)
        for (call in callsToAddArguments) {
            call.addAfter(psiFactory.createTypeArguments("<$typeArguments>"), call.calleeExpression)
            ShortenReferences.DEFAULT.process(call.typeArgumentList!!)
        }
    }

    private fun getTypeArgumentsStringForCall(initializer: KtExpression): String? {
        val context = initializer.analyze(BodyResolveMode.PARTIAL)
        val call = initializer.getResolvedCall(context) ?: return null
        val typeArgumentMap = call.typeArguments
        val typeArguments = call.candidateDescriptor.typeParameters.mapNotNull { typeArgumentMap[it] }
        return typeArguments.joinToString { IdeDescriptorRenderers.SOURCE_CODE_FOR_TYPE_ARGUMENTS.renderType(it) }
    }

    private fun hasIncompleteTypeInferenceDiagnostic(call: Call, context: BindingContext): Boolean {
        val callee = call.calleeExpression ?: return false
        return context.diagnostics.forElement(callee).any { it.factory == Errors.TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER }
    }
}
