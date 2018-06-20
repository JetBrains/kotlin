/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.refactoring.rename.KotlinVariableInplaceRenameHandler
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getOrCreateParameterList
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypesAndPredicate
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class NestedLambdaShadowedImplicitParameterInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return lambdaExpressionVisitor(fun(lambda: KtLambdaExpression) {
            val context: BindingContext by lazy { lambda.analyze(BodyResolveMode.PARTIAL) }
            if (lambda.valueParameters.isNotEmpty() || lambda.functionDescriptor(context)?.valueParameters?.size != 1) return
            if (lambda.getParentImplicitParameterLambda(context) == null) return
            val callee = lambda.getStrictParentOfType<KtCallExpression>()?.calleeExpression ?: return
            holder.registerProblem(
                callee,
                "Implicit parameter 'it' of enclosing lambda is shadowed",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                AddExplicitParameterNameFix()
            )
        })
    }

    private class AddExplicitParameterNameFix : LocalQuickFix {
        override fun getName() = "Add explicit parameter name to outer lambda"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val parentLambda = (descriptor.psiElement as? KtExpression)?.getParentImplicitParameterLambda() ?: return
            val parameter = parentLambda.functionLiteral.getOrCreateParameterList().addParameterBefore(
                KtPsiFactory(project).createLambdaParameterList("it").parameters.first(), null
            )
            val editor = parentLambda.findExistingEditor()
            if (editor != null) {
                PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)
                editor.caretModel.moveToOffset(parameter.startOffset)
                KotlinVariableInplaceRenameHandler().doRename(parameter, editor, null)
            }
        }
    }
}

private fun KtLambdaExpression.functionDescriptor(context: BindingContext) = context[BindingContext.FUNCTION, functionLiteral]

private fun KtExpression.getParentImplicitParameterLambda(
    context: BindingContext = this.analyze(BodyResolveMode.PARTIAL)
): KtLambdaExpression? {
    return getParentOfTypesAndPredicate(true, KtLambdaExpression::class.java) {
        it.valueParameters.isEmpty() && it.functionDescriptor(context)?.valueParameters?.size == 1
    }
}
