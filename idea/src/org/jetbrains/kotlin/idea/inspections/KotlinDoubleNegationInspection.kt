/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.prefixExpressionVisitor
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.types.typeUtil.isBoolean

class KotlinDoubleNegationInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession) =
            prefixExpressionVisitor(fun(expression) {
                if (expression.operationToken != KtTokens.EXCL ||
                    expression.baseExpression?.getType(expression.analyze())?.isBoolean() != true) {
                    return
                }
                var parent = expression.parent
                while (parent is KtParenthesizedExpression) {
                    parent = parent.parent
                }
                if (parent is KtPrefixExpression && parent.operationToken == KtTokens.EXCL) {
                    holder.registerProblem(expression,
                                           "Redundant double negation",
                                           ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                           DoubleNegationFix())
                }
            })

    private class DoubleNegationFix : LocalQuickFix {
        override fun getName() = "Remove redundant negations"
        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            applyFix(descriptor.psiElement as? KtPrefixExpression ?: return)
        }

        private fun applyFix(expression: KtPrefixExpression) {
            var parent = expression.parent
            while (parent is KtParenthesizedExpression) {
                parent = parent.parent
            }
            if (parent is KtPrefixExpression && parent.operationToken == KtTokens.EXCL) {
                expression.baseExpression?.let { parent.replaced(it) }
            }
        }
    }
}