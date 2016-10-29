/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.intentions.negate
import org.jetbrains.kotlin.psi.*

class RedundantIfInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitIfExpression(expression: KtIfExpression) {
                super.visitIfExpression(expression)
                expression.condition ?: return
                val type = isRedundant(expression)
                if (type == RedundantType.NONE) return
                holder.registerProblem(expression,
                                       "Redundant 'if' statement",
                                       ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                                       RemoveRedundantIf(type))
            }
        }
    }

    private enum class RedundantType {
        NONE,
        THEN_TRUE,
        ELSE_TRUE
    }

    private fun isRedundant(expression: KtIfExpression): RedundantType {
        val thenReturn = getReturnedExpression(expression.then) ?: return RedundantType.NONE
        val elseReturn = getReturnedExpression(expression.`else`) ?: return RedundantType.NONE

        if (KtPsiUtil.isTrueConstant(thenReturn) && KtPsiUtil.isFalseConstant(elseReturn)) {
            return RedundantType.THEN_TRUE
        }

        if (KtPsiUtil.isFalseConstant(thenReturn) && KtPsiUtil.isTrueConstant(elseReturn)) {
            return RedundantType.ELSE_TRUE
        }

        return RedundantType.NONE
    }

    private fun getReturnedExpression(expression: KtExpression?) : KtExpression? {
        when(expression) {
            is KtReturnExpression -> return expression.returnedExpression
            is KtBlockExpression -> {
                val statement = expression.statements.singleOrNull() as? KtReturnExpression ?: return null
                return statement.returnedExpression
            }
            else -> return null
        }
    }

    private class RemoveRedundantIf(val redundantType: RedundantType) : LocalQuickFix {
        override fun getName() = "Remove redundant 'if' statement"
        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement as KtIfExpression
            val condition = when (redundantType) {
                RedundantType.NONE -> return
                RedundantType.THEN_TRUE -> element.condition!!
                RedundantType.ELSE_TRUE -> element.condition!!.negate()
            }
            element.replace(KtPsiFactory(element).createExpressionByPattern("return $0", condition.text))
        }
    }

}
