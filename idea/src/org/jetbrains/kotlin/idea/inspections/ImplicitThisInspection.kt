/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.util.getFactoryForImplicitReceiverWithSubtypeOf
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall

class ImplicitThisInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : KtVisitorVoid() {

        override fun visitCallableReferenceExpression(expression: KtCallableReferenceExpression) {
            if (expression.receiverExpression != null) return

            handle(expression, expression.callableReference, ::CallableReferenceFix)
        }

        override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
            if (expression !is KtNameReferenceExpression) return
            if (expression.parent is KtThisExpression) return
            if (expression.parent is KtCallableReferenceExpression) return
            if (expression.isSelectorOfDotQualifiedExpression()) return
            val parent = expression.parent
            if (parent is KtCallExpression && parent.isSelectorOfDotQualifiedExpression()) return

            handle(expression, expression, ::CallFix)
        }

        private fun handle(expression: KtExpression, reference: KtReferenceExpression, fixFactory: (String) -> LocalQuickFix) {
            val context = reference.analyze()
            val scope = reference.getResolutionScope(context) ?: return

            val resolvedCall = reference.getResolvedCall(context) ?: return
            val variableDescriptor = (resolvedCall as? VariableAsFunctionResolvedCall)?.variableCall?.resultingDescriptor
            val callableDescriptor = resolvedCall.resultingDescriptor
            val receiverDescriptor = variableDescriptor?.receiverDescriptor() ?: callableDescriptor.receiverDescriptor() ?: return
            val receiverType = receiverDescriptor.type

            val expressionFactory = scope.getFactoryForImplicitReceiverWithSubtypeOf(receiverType) ?: return
            val receiverText = if (expressionFactory.isImmediate) "this" else expressionFactory.expressionText

            val fix = fixFactory(receiverText)
            holder.registerProblem(
                expression,
                KotlinBundle.message("callable.reference.fix.family.name", receiverText),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                fix
            )
        }

        private fun CallableDescriptor.receiverDescriptor(): ReceiverParameterDescriptor? {
            return extensionReceiverParameter ?: dispatchReceiverParameter
        }

        private fun KtExpression.isSelectorOfDotQualifiedExpression(): Boolean {
            val parent = parent
            return parent is KtDotQualifiedExpression && parent.selectorExpression == this
        }
    }

    private class CallFix(private val receiverText: String) : LocalQuickFix {
        override fun getFamilyName() = KotlinBundle.message("callable.reference.fix.family.name", receiverText)

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val expression = descriptor.psiElement as? KtExpression ?: return
            val factory = KtPsiFactory(project)
            val call = expression.parent as? KtCallExpression ?: expression

            call.replace(factory.createExpressionByPattern("$0.$1", receiverText, call))
        }
    }

    private class CallableReferenceFix(private val receiverText: String) : LocalQuickFix {
        override fun getFamilyName() = KotlinBundle.message("callable.reference.fix.family.name", receiverText)

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val expression = descriptor.psiElement as? KtCallableReferenceExpression ?: return
            val factory = KtPsiFactory(project)
            val reference = expression.callableReference

            expression.replace(factory.createExpressionByPattern("$0::$1", receiverText, reference))
        }
    }
}