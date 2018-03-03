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
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.psi.*

class RemoveRedundantConstructorParenthesesInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return callExpressionVisitor { expression ->
            val valueArguments = expression.valueArgumentList ?: return@callExpressionVisitor
            if (valueArguments.arguments.size != 1) return@callExpressionVisitor
            valueArguments.arguments.mapNotNull { it.getArgumentExpression() }.singleOrNull() as? KtLambdaExpression ?: return@callExpressionVisitor

            holder.registerProblem(
                    expression,
                    "Remove redundant constructor parentheses",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    RemoveRedundantConstructorParenthesesQuickfix()
            )
        }
    }
}

class RemoveRedundantConstructorParenthesesQuickfix : LocalQuickFix {
    override fun getName() = "Remove redundant constructor parentheses"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val callExpression = descriptor.psiElement as? KtCallExpression ?: return
        val lambdaExpression = callExpression.valueArguments.mapNotNull { it.getArgumentExpression() }.singleOrNull() as? KtLambdaExpression ?: return
        val calleeExpression = callExpression.calleeExpression ?: return
        val lambdaBodyExpression = lambdaExpression.bodyExpression ?: return

        callExpression.replace(KtPsiFactory(project).createExpressionByPattern("$0 { $1 }", calleeExpression, lambdaBodyExpression))
    }
}