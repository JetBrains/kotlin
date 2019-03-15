/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.refactoring.replaceWithCopyWithResolveCheck
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.isSingleUnderscore

class RedundantLambdaArrowInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return lambdaExpressionVisitor(fun(lambdaExpression: KtLambdaExpression) {
            val functionLiteral = lambdaExpression.functionLiteral
            val arrow = functionLiteral.arrow ?: return
            val parameters = functionLiteral.valueParameters
            val singleParameter = parameters.singleOrNull()
            if (parameters.isNotEmpty() && singleParameter?.isSingleUnderscore != true && singleParameter?.name != "it") {
                return
            }

            if (lambdaExpression.getStrictParentOfType<KtWhenEntry>()?.expression == lambdaExpression) return
            if (lambdaExpression.getStrictParentOfType<KtContainerNodeForControlStructureBody>()?.let {
                    it.node.elementType in listOf(KtNodeTypes.THEN, KtNodeTypes.ELSE) && it.expression == lambdaExpression
                } == true) return

            val callExpression = lambdaExpression.parent?.parent as? KtCallExpression
            if (callExpression != null) {
                val callee = callExpression.calleeExpression as? KtNameReferenceExpression
                if (callee != null && callee.getReferencedName() == "forEach" && singleParameter?.name != "it") return
            }

            val lambdaContext = lambdaExpression.analyze()
            if (parameters.isNotEmpty() && lambdaContext[BindingContext.EXPECTED_EXPRESSION_TYPE, lambdaExpression] == null) return

            val valueArgument = lambdaExpression.parent as? KtValueArgument
            val valueArgumentCall = valueArgument?.getStrictParentOfType<KtCallExpression>()
            if (valueArgumentCall?.isApplicableCall(lambdaExpression, lambdaContext) == false) return

            val functionLiteralDescriptor = functionLiteral.descriptor
            if (functionLiteralDescriptor != null) {
                if (functionLiteral.anyDescendantOfType<KtNameReferenceExpression> {
                        it.text == "it" && it.resolveToCall()?.resultingDescriptor?.containingDeclaration != functionLiteralDescriptor
                    }) return
            }

            val startOffset = functionLiteral.startOffset
            holder.registerProblem(
                holder.manager.createProblemDescriptor(
                    functionLiteral,
                    TextRange((singleParameter?.startOffset ?: arrow.startOffset) - startOffset, arrow.endOffset - startOffset),
                    "Redundant lambda arrow",
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    isOnTheFly,
                    DeleteFix()
                )
            )
        })
    }

    class DeleteFix : LocalQuickFix {
        override fun getFamilyName() = "Remove arrow"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement as? KtFunctionLiteral ?: return
            FileModificationService.getInstance().preparePsiElementForWrite(element)
            element.removeArrow()
        }
    }
}

private fun KtCallExpression.isApplicableCall(lambdaExpression: KtLambdaExpression, lambdaContext: BindingContext): Boolean {
    val offset = lambdaExpression.textOffset - textOffset
    val dotQualifiedExpression = parent as? KtDotQualifiedExpression
    return if (dotQualifiedExpression == null) {
        replaceWithCopyWithResolveCheck(
            resolveStrategy = { expr, context ->
                expr.getResolvedCall(context)?.resultingDescriptor
            },
            context = lambdaContext,
            preHook = {
                findLambdaExpressionByOffset(offset)?.functionLiteral?.removeArrow()
            }
        )
    } else {
        dotQualifiedExpression.replaceWithCopyWithResolveCheck(
            resolveStrategy = { expr, context ->
                expr.selectorExpression.getResolvedCall(context)?.resultingDescriptor
            },
            context = lambdaContext,
            preHook = {
                val call = selectorExpression as? KtCallExpression
                call?.findLambdaExpressionByOffset(offset)?.functionLiteral?.removeArrow()
            }
        )
    } != null
}

private fun KtFunctionLiteral.removeArrow() {
    valueParameterList?.delete()
    arrow?.delete()
}

private fun KtCallExpression.findLambdaExpressionByOffset(offset: Int): KtLambdaExpression? =
    lambdaArguments.asSequence().mapNotNull(KtLambdaArgument::getLambdaExpression).firstOrNull { it.textOffset == offset }
        ?: valueArguments.asSequence().mapNotNull(KtValueArgument::getArgumentExpression).firstOrNull { it.textOffset == offset } as? KtLambdaExpression

