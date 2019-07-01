/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver

class SelfAssignmentInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return binaryExpressionVisitor(fun(expression) {
            if (expression.operationToken != KtTokens.EQ) return
            val left = expression.left
            val leftRefExpr = left?.asNameReferenceExpression() ?: return
            val right = expression.right
            val rightRefExpr = right?.asNameReferenceExpression() ?: return
            // To omit analyzing too much
            if (leftRefExpr.text != rightRefExpr.text) return

            val context = expression.analyze(BodyResolveMode.PARTIAL)
            val leftResolvedCall = left.getResolvedCall(context)
            val leftCallee = leftResolvedCall?.resultingDescriptor as? VariableDescriptor ?: return
            val rightResolvedCall = right.getResolvedCall(context)
            val rightCallee = rightResolvedCall?.resultingDescriptor as? VariableDescriptor ?: return
            if (leftCallee != rightCallee) return

            if (!rightCallee.isVar) return
            if (rightCallee is PropertyDescriptor) {
                if (rightCallee.isOverridable) return
                if (rightCallee.accessors.any { !it.isDefault }) return
            }

            if (left.receiverDeclarationDescriptor(leftResolvedCall, context) !=
                right.receiverDeclarationDescriptor(rightResolvedCall, context)) {
                return
            }

            holder.registerProblem(right,
                                   "Variable '${rightCallee.name}' is assigned to itself",
                                   ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                   RemoveSelfAssignmentFix())
        })
    }

    private fun KtExpression.asNameReferenceExpression(): KtNameReferenceExpression? = when (this) {
        is KtNameReferenceExpression ->
            this
        is KtDotQualifiedExpression ->
            (selectorExpression as? KtNameReferenceExpression)?.takeIf { receiverExpression is KtThisExpression }
        else ->
            null
    }

    private fun KtExpression.receiverDeclarationDescriptor(
        resolvedCall: ResolvedCall<out CallableDescriptor>,
        context: BindingContext
    ): DeclarationDescriptor? {
        val thisExpression = (this as? KtDotQualifiedExpression)?.receiverExpression as? KtThisExpression
        if (thisExpression != null) {
            return thisExpression.getResolvedCall(context)?.resultingDescriptor?.containingDeclaration
        }
        val implicitReceiver = with (resolvedCall) { dispatchReceiver ?: extensionReceiver } as? ImplicitReceiver
        return implicitReceiver?.declarationDescriptor
    }
}

private class RemoveSelfAssignmentFix : LocalQuickFix {
    override fun getName() = "Remove self assignment"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val right = descriptor.psiElement as? KtExpression ?: return
        right.parent.delete()
    }
}
