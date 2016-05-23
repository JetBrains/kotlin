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
import org.jetbrains.kotlin.psi.*

class RedundantIfInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitIfExpression(expression: KtIfExpression) {
                super.visitIfExpression(expression)

                if (expression.condition != null && isRedundant(expression)) {
                    holder.registerProblem(expression,
                                           "Redundant 'if' statement",
                                           ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                                           RemoveRedundantIf)
                }
            }
        }
    }

    private fun isRedundant(expression: KtIfExpression): Boolean {
        val thenReturn = getReturnedExpression(expression.then) ?: return false
        val elseReturn = getReturnedExpression(expression.`else`) ?: return false

        if (KtPsiUtil.isTrueConstant(thenReturn) && KtPsiUtil.isFalseConstant(elseReturn)) {
            return true
        }

        if (KtPsiUtil.isFalseConstant(thenReturn) && KtPsiUtil.isTrueConstant(elseReturn)) {
            return true
        }

        return false
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

    private object RemoveRedundantIf : LocalQuickFix {
        override fun getName() = "Remove redundant 'if' statement"
        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement as KtIfExpression
            element.replace(KtPsiFactory(element).createExpressionByPattern("return $0", element.condition!!.text));
        }
    }

}
