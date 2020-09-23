/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
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
            val lambda = valueArguments[1].lambdaExpression() ?: return
            val lambdaBody = lambda.bodyExpression ?: return

            val context = callExpression.analyze(BodyResolveMode.PARTIAL_WITH_CFA)
            if (lambdaBody.statements.size > 1 && callExpression.isUsedAsExpression(context)) return
            if (callExpression.getResolvedCall(context)?.resultingDescriptor?.fqNameSafe != FqName("kotlin.with")) return

            val lambdaDescriptor = context[BindingContext.FUNCTION, lambda.functionLiteral] ?: return

            var used = false
            lambda.functionLiteral.acceptChildren(object : KtVisitorVoid() {
                override fun visitKtElement(element: KtElement) {
                    if (used) return
                    element.acceptChildren(this)

                    if (element is KtReturnExpression && element.getLabelName() == "with") {
                        used = true
                        return
                    }

                    val resolvedCall = element.getResolvedCall(context) ?: return

                    if (isUsageOfDescriptor(lambdaDescriptor, resolvedCall, context)) {
                        used = true
                    }
                }
            })

            if (!used) {
                val quickfix = when (receiver) {
                    is KtSimpleNameExpression, is KtStringTemplateExpression, is KtConstantExpression -> RemoveRedundantWithFix()
                    else -> null
                }
                holder.registerProblem(
                    callee,
                    KotlinBundle.message("redundant.with.call"),
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    quickfix
                )
            }
        })
}

private fun KtValueArgument.lambdaExpression(): KtLambdaExpression? =
    (this as? KtLambdaArgument)?.getLambdaExpression() ?: this.getArgumentExpression() as? KtLambdaExpression

private class RemoveRedundantWithFix : LocalQuickFix {
    override fun getName() = KotlinBundle.message("remove.redundant.with.fix.text")

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val callExpression = descriptor.psiElement.parent as? KtCallExpression ?: return
        val lambdaExpression = callExpression.valueArguments.getOrNull(1)?.lambdaExpression() ?: return
        val lambdaBody = lambdaExpression.bodyExpression ?: return
        val declaration = callExpression.getStrictParentOfType<KtDeclarationWithBody>()
        val replaced = if (declaration?.equalsToken != null && KtPsiUtil.deparenthesize(declaration.bodyExpression) == callExpression) {
            val singleReturnedExpression = (lambdaBody.statements.singleOrNull() as? KtReturnExpression)?.returnedExpression
            if (singleReturnedExpression != null) {
                callExpression.replaced(singleReturnedExpression)
            } else {
                declaration.equalsToken?.delete()
                declaration.bodyExpression?.replaced(KtPsiFactory(project).createSingleStatementBlock(lambdaBody))
            }
        } else {
            callExpression.replaced(lambdaBody)
        }
        if (replaced != null) {
            replaced.findExistingEditor()?.moveCaret(replaced.startOffset)
        }
    }
}
