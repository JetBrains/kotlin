/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.intentions.isArrayOfMethod
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class RemoveRedundantSpreadOperatorInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return valueArgumentVisitor(fun(argument) {
            val spreadElement = argument.getSpreadElement() ?: return
            if (argument.isNamed()) return
            val argumentExpression = argument.getArgumentExpression() ?: return
            val argumentOffset = argument.startOffset
            val startOffset = spreadElement.startOffset - argumentOffset
            val endOffset =
                when (argumentExpression) {
                    is KtCallExpression -> {
                        if (!argumentExpression.isArrayOfMethod()) return
                        argumentExpression.calleeExpression!!.endOffset - argumentOffset
                    }
                    is KtCollectionLiteralExpression -> startOffset + 1
                    else -> return
                }

            val problemDescriptor = holder.manager.createProblemDescriptor(
                argument,
                TextRange(startOffset, endOffset),
                "Remove redundant spread operator",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                isOnTheFly,
                RemoveRedundantSpreadOperatorQuickfix()
            )
            holder.registerProblem(problemDescriptor)
        })
    }
}

class RemoveRedundantSpreadOperatorQuickfix : LocalQuickFix {
    override fun getName() = "Remove redundant spread operator"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        // Argument & expression under *
        val spreadValueArgument = descriptor.psiElement as? KtValueArgument ?: return
        val spreadArgumentExpression = spreadValueArgument.getArgumentExpression() ?: return
        val outerArgumentList = spreadValueArgument.getStrictParentOfType<KtValueArgumentList>() ?: return
        // Arguments under arrayOf or []
        val innerArgumentExpressions =
            when (spreadArgumentExpression) {
                is KtCallExpression -> spreadArgumentExpression.valueArgumentList?.arguments?.map {
                    it.getArgumentExpression() to it.isSpread
                }
                is KtCollectionLiteralExpression -> spreadArgumentExpression.getInnerExpressions().map { it to false }
                else -> null
            } ?: return

        val factory = KtPsiFactory(project)
        innerArgumentExpressions.reversed().forEach { (expression, isSpread) ->
            outerArgumentList.addArgumentAfter(factory.createArgument(expression, isSpread = isSpread), spreadValueArgument)
        }
        outerArgumentList.removeArgument(spreadValueArgument)
    }
}