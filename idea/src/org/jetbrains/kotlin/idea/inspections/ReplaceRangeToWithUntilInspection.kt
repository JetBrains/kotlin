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

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.intentions.getArguments
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class ReplaceRangeToWithUntilInspection : AbstractPrimitiveRangeToInspection() {
    override fun visitRangeToExpression(expression: KtExpression, holder: ProblemsHolder) {
        if (expression.getArguments()?.second?.deparenthesize()?.isMinusOne() != true) return

        holder.registerProblem(
            expression,
            "'rangeTo' or the '..' call should be replaced with 'until'",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            ReplaceWithUntilQuickFix()
        )
    }

    class ReplaceWithUntilQuickFix : LocalQuickFix {
        override fun getName() = "Replace with until"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement as KtExpression
            val args = element.getArguments() ?: return
            element.replace(
                KtPsiFactory(element).createExpressionByPattern(
                    "$0 until $1",
                    args.first ?: return,
                    (args.second?.deparenthesize() as? KtBinaryExpression)?.left ?: return
                )
            )
        }
    }

    private fun KtExpression.isMinusOne(): Boolean {
        if (this !is KtBinaryExpression) return false
        if (operationToken != KtTokens.MINUS) return false

        val constantValue = right?.constantValueOrNull()
        val rightValue = (constantValue?.value as? Number)?.toInt() ?: return false
        return rightValue == 1
    }
}

private fun KtExpression.deparenthesize() = KtPsiUtil.safeDeparenthesize(this)
