/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.collections

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.core.setType
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getReturnTypeReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.typeUtil.makeNullable

class ConvertMapWithNotNullAssertionToMapNotNullInspection : AbstractKotlinInspection() {
    companion object {
        private const val MAP = "map"
        private val MAP_FQ_NAME = FqName("kotlin.collections.map")
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = callExpressionVisitor(fun(call) {
        val callee = call.calleeExpression ?: return
        if (callee.text != MAP) return
        val context = call.analyze(BodyResolveMode.PARTIAL)
        if (call.getResolvedCall(context)?.isCalling(MAP_FQ_NAME) != true) return

        val postfix = call.lastExpressionWithNotNullAssertionInArgumentBlock() ?: return
        val returnExpression = postfix.getStrictParentOfType<KtReturnExpression>()
        if (returnExpression != null) {
            val functionLiteral = returnExpression.getStrictParentOfType<KtFunctionLiteral>()
            if (functionLiteral != null) {
                val targetLabel = returnExpression.getTargetLabel() ?: return
                if (returnExpression.analyze(BodyResolveMode.PARTIAL)[BindingContext.LABEL_TARGET, targetLabel] != functionLiteral) return
            }
        }

        holder.registerProblem(
            callee,
            "'map' with not-null assertion should be simplified to 'mapNotNull'",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            ConvertToMapNotNullFix()
        )
    })

    private class ConvertToMapNotNullFix : LocalQuickFix, LowPriorityAction {
        override fun getName() = "Convert to 'mapNotNull'"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val call = descriptor.psiElement.parent as? KtCallExpression ?: return
            val callee = call.calleeExpression ?: return
            val postfix = call.lastExpressionWithNotNullAssertionInArgumentBlock() ?: return
            val base = postfix.baseExpression ?: return
            val anonymousFunction = call.valueArguments.firstOrNull()?.getArgumentExpression() as? KtNamedFunction
            val psiFactory = KtPsiFactory(postfix)

            callee.replace(psiFactory.createExpression("mapNotNull") as KtNameReferenceExpression)

            val replacedPostfix = postfix.replaced(base)

            val returnExpression = replacedPostfix.getStrictParentOfType<KtReturnExpression>()
            val returnedExpression = returnExpression?.returnedExpression
            if (returnExpression?.getLabelName() == "map" && returnedExpression != null) {
                returnExpression.replace(psiFactory.createExpressionByPattern("return@mapNotNull $0", returnedExpression))
            }

            if (anonymousFunction != null) {
                val returnType = anonymousFunction.getReturnTypeReference()?.let {
                    it.analyze(BodyResolveMode.PARTIAL)[BindingContext.TYPE, it]
                }
                if (returnType != null) {
                    anonymousFunction.setType(returnType.makeNullable())
                }
            }
        }
    }
}

private fun KtCallExpression.lastExpressionWithNotNullAssertionInArgumentBlock(): KtPostfixExpression? {
    val lastStatement = when (val argument = this.valueArguments.singleOrNull()) {
        is KtLambdaArgument -> argument.getLambdaExpression()?.functionLiteral?.bodyBlockExpression
        else -> (argument?.getArgumentExpression() as? KtNamedFunction)?.bodyBlockExpression
    }?.statements?.lastOrNull()?.let { KtPsiUtil.safeDeparenthesize(it) } ?: return null

    val postfix = when (lastStatement) {
        is KtReturnExpression -> lastStatement.returnedExpression?.let { KtPsiUtil.safeDeparenthesize(it) }
        else -> lastStatement
    } as? KtPostfixExpression ?: return null
    if (postfix.baseExpression == null || postfix.operationReference.getReferencedNameElementType() != KtTokens.EXCLEXCL) return null

    return postfix
}
