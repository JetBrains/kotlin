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
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.util.getReceiverTargetDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class RedundantWithInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        callExpressionVisitor(fun(callExpression) {
            val callee = callExpression.calleeExpression ?: return
            if (callee.text != "with") return

            val valueArguments = callExpression.valueArguments
            if (valueArguments.size != 2) return
            val receiver = valueArguments[0].getArgumentExpression() ?: return
            if (receiver !is KtSimpleNameExpression && receiver !is KtStringTemplateExpression && receiver !is KtConstantExpression) return
            val lambda = valueArguments[1].lambdaExpression() ?: return
            val lambdaBody = lambda.bodyExpression ?: return

            val parent = callExpression.parent
            if ((parent is KtValueArgument
                        || parent is KtReturnExpression
                        || parent is KtOperationExpression
                        || (parent as? KtDeclarationWithInitializer)?.initializer == callExpression)
                && lambdaBody.statements.size > 1
            ) return

            val context = callExpression.analyze(BodyResolveMode.PARTIAL)
            if (callExpression.getResolvedCall(context)?.resultingDescriptor?.fqNameSafe != FqName("kotlin.with")) return
            val lambdaDescriptor = context[BindingContext.FUNCTION, lambda.functionLiteral] ?: return
            val receiverClassDescriptor =
                (receiver.getResolvedCall(context)?.resultingDescriptor as? FakeCallableDescriptorForObject)?.classDescriptor
            val lambdaDispatchReceiver = lambdaDescriptor.dispatchReceiverParameter
            val lambdaExtensionReceiver = lambdaDescriptor.extensionReceiverParameter
            val needCall = lambda.anyDescendantOfType<KtExpression> {
                when (it) {
                    is KtThisExpression -> {
                        val thisDescriptor = it.getResolvedCall(context)?.resultingDescriptor ?: return@anyDescendantOfType false
                        thisDescriptor == lambdaDispatchReceiver || thisDescriptor == lambdaExtensionReceiver
                    }
                    is KtSimpleNameExpression -> {
                        if (it is KtOperationReferenceExpression) return@anyDescendantOfType false
                        val resolvedCall = it.getResolvedCall(context) ?: return@anyDescendantOfType false
                        val dispatchReceiverTarget = resolvedCall.dispatchReceiver?.getReceiverTargetDescriptor(context)
                        val extensionReceiverTarget = resolvedCall.extensionReceiver?.getReceiverTargetDescriptor(context)
                        lambdaDescriptor == dispatchReceiverTarget || lambdaDescriptor == extensionReceiverTarget ||
                                receiverClassDescriptor != null && receiverClassDescriptor == extensionReceiverTarget?.containingDeclaration
                    }
                    is KtReturnExpression -> it.labelQualifier?.text == "@with"
                    else -> false
                }
            }
            if (!needCall) {
                holder.registerProblem(
                    callee,
                    "Redundant 'with' call",
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    RemoveRedundantWithFix()
                )
            }
        })
}

private fun KtValueArgument.lambdaExpression(): KtLambdaExpression? =
    (this as? KtLambdaArgument)?.getLambdaExpression() ?: this.getArgumentExpression() as? KtLambdaExpression

private class RemoveRedundantWithFix : LocalQuickFix {
    override fun getName() = "Remove redundant 'with' call"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val callExpression = descriptor.psiElement.parent as? KtCallExpression ?: return
        val lambdaBody = callExpression.valueArguments.getOrNull(1)?.lambdaExpression()?.bodyExpression ?: return
        val replaced = callExpression.replaced(lambdaBody)
        replaced.findExistingEditor()?.moveCaret(replaced.startOffset)
    }
}
