/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.types.typeUtil.isBoolean

class KotlinDoubleNegationInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession) =
            object : KtVisitorVoid() {
                override fun visitPrefixExpression(expression: KtPrefixExpression) {
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
                }
            }

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