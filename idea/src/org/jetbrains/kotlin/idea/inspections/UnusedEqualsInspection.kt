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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.evaluatesTo
import org.jetbrains.kotlin.idea.refactoring.getLineNumber
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.lastBlockStatementOrThis
import org.jetbrains.kotlin.resolve.calls.callUtil.getType

class UnusedEqualsInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitBinaryExpression(expression: KtBinaryExpression) {
                super.visitBinaryExpression(expression)
                if (expression.operationToken != KtTokens.EQEQ) return
                val parent = expression.parent as? KtBlockExpression ?: return
                val lastBlockStatement = parent.lastBlockStatementOrThis()
                when {
                    expression.evaluatesTo(lastBlockStatement) && expression.getLineNumber() == lastBlockStatement.getLineNumber() ->
                        expression.getStrictParentOfType<KtLambdaExpression>()?.visitLambdaExpression(holder, expression) ?: holder.registerUnusedEqualsProblem(expression)
                    else -> holder.registerUnusedEqualsProblem(expression)
                }
            }

            private fun KtLambdaExpression.visitLambdaExpression(holder: ProblemsHolder, expression: KtBinaryExpression) {
                val lambdaTypeArguments = getType(analyze())?.arguments ?: return
                if (lambdaTypeArguments.size != 1) return
                when {
                    KotlinBuiltIns.isBoolean(lambdaTypeArguments[0].type) -> {
                        val lastBlockStatementOrThis = bodyExpression?.lastBlockStatementOrThis() ?: return
                        if (!expression.evaluatesTo(lastBlockStatementOrThis)) holder.registerUnusedEqualsProblem(expression)
                    }
                    else -> holder.registerUnusedEqualsProblem(expression)
                }
            }

            private fun ProblemsHolder.registerUnusedEqualsProblem(expression: KtBinaryExpression) {
                registerProblem(expression,
                                "Unused equals expression",
                                ProblemHighlightType.LIKE_UNUSED_SYMBOL)
            }
        }
    }

}