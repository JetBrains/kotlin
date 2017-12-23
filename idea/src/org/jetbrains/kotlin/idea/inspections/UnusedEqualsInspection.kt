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

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.conventionNameCalls.isAnyEquals
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.util.OperatorNameConventions

class UnusedEqualsInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            private fun reportIfNotUsedAsExpression(expression: KtExpression, target: KtExpression) {
                val context = expression.analyze()
                if (!expression.isUsedAsExpression(context)) {
                    holder.registerProblem(target,
                                           "Unused equals expression",
                                           ProblemHighlightType.LIKE_UNUSED_SYMBOL)

                }
            }

            override fun visitBinaryExpression(expression: KtBinaryExpression) {
                super.visitBinaryExpression(expression)
                if (expression.operationToken == KtTokens.EQEQ &&
                    (expression.parent is KtBlockExpression || expression.parent.parent is KtIfExpression)) {
                    reportIfNotUsedAsExpression(expression, expression.operationReference)
                }
            }

            override fun visitCallExpression(expression: KtCallExpression) {
                super.visitCallExpression(expression)

                val calleeExpression = expression.calleeExpression as? KtSimpleNameExpression ?: return
                if (calleeExpression.getReferencedNameAsName() != OperatorNameConventions.EQUALS) return

                if (!expression.isAnyEquals()) return
                reportIfNotUsedAsExpression(expression.getQualifiedExpressionForSelectorOrThis(), calleeExpression)
            }
        }
    }

}