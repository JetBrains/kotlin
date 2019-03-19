/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.inspections.collections.isCalling
import org.jetbrains.kotlin.idea.intentions.ReplaceItWithExplicitFunctionLiteralParamIntention
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.refactoring.rename.KotlinVariableInplaceRenameHandler
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall

class NestedLambdaShadowedImplicitParameterInspection : AbstractKotlinInspection() {
    companion object {
        val scopeFunctions = listOf(
            "kotlin.also",
            "kotlin.let",
            "kotlin.takeIf",
            "kotlin.takeUnless"
        ).map { FqName(it) }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return lambdaExpressionVisitor(fun(lambda: KtLambdaExpression) {
            if (lambda.valueParameters.isNotEmpty()) return
            if (lambda.getStrictParentOfType<KtLambdaExpression>() == null) return

            val context = lambda.analyze()
            val implicitParameter = lambda.getImplicitParameter(context) ?: return
            if (lambda.getParentImplicitParameterLambda(context) == null) return

            val qualifiedExpression = lambda.getStrictParentOfType<KtQualifiedExpression>()
            if (qualifiedExpression != null) {
                val receiver = qualifiedExpression.receiverExpression
                val call = qualifiedExpression.callExpression
                if (receiver.text == "it" && call?.isCalling(scopeFunctions, context) == true) return
            }

            val containingFile = lambda.containingFile
            lambda.forEachDescendantOfType<KtNameReferenceExpression> {
                if (it.isImplicitParameterReference(lambda, implicitParameter, context)) {
                    holder.registerProblem(
                        it,
                        "Implicit parameter 'it' of enclosing lambda is shadowed",
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        AddExplicitParameterToOuterLambdaFix(),
                        IntentionWrapper(ReplaceItWithExplicitFunctionLiteralParamIntention(), containingFile)
                    )
                }
            }
        })
    }

    private class AddExplicitParameterToOuterLambdaFix : LocalQuickFix {
        override fun getName() = "Add explicit parameter name to outer lambda"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val implicitParameterReference = descriptor.psiElement as? KtNameReferenceExpression ?: return
            val lambda = implicitParameterReference.getStrictParentOfType<KtLambdaExpression>() ?: return
            val parentLambda = lambda.getParentImplicitParameterLambda() ?: return
            val parameter = parentLambda.functionLiteral.getOrCreateParameterList().addParameterBefore(
                KtPsiFactory(project).createLambdaParameterList("it").parameters.first(), null
            )
            val editor = parentLambda.findExistingEditor() ?: return
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)
            editor.caretModel.moveToOffset(parameter.startOffset)
            KotlinVariableInplaceRenameHandler().doRename(parameter, editor, null)
        }
    }
}

private fun KtLambdaExpression.getImplicitParameter(context: BindingContext): ValueParameterDescriptor? {
    return context[BindingContext.FUNCTION, functionLiteral]?.valueParameters?.singleOrNull()
}

private fun KtLambdaExpression.getParentImplicitParameterLambda(context: BindingContext = this.analyze()): KtLambdaExpression? {
    return getParentOfTypesAndPredicate(true, KtLambdaExpression::class.java) { lambda ->
        if (lambda.valueParameters.isNotEmpty()) return@getParentOfTypesAndPredicate false
        val implicitParameter = lambda.getImplicitParameter(context) ?: return@getParentOfTypesAndPredicate false
        lambda.anyDescendantOfType<KtNameReferenceExpression> {
            it.isImplicitParameterReference(lambda, implicitParameter, context)
        }
    }
}

private fun KtNameReferenceExpression.isImplicitParameterReference(
    lambda: KtLambdaExpression,
    implicitParameter: ValueParameterDescriptor,
    context: BindingContext
): Boolean {
    return text == "it"
            && getStrictParentOfType<KtLambdaExpression>() == lambda
            && getResolvedCall(context)?.resultingDescriptor == implicitParameter
}