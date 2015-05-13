/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.JetNodeTypes
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingOffsetIndependentIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.*
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.replaced

public class IfThenToElvisInspection : IntentionBasedInspection<JetIfExpression>(IfThenToElvisIntention())

public class IfThenToElvisIntention : JetSelfTargetingOffsetIndependentIntention<JetIfExpression>(javaClass(), "Replace 'if' expression with elvis expression") {

    override fun isApplicableTo(element: JetIfExpression): Boolean {
        val condition = element.getCondition() as? JetBinaryExpression ?: return false
        val thenClause = element.getThen() ?: return false
        val elseClause = element.getElse() ?: return false

        val expression = condition.expressionComparedToNull() ?: return false
        if (!expression.isStableVariable()) return false

        return when (condition.getOperationToken()) {
            JetTokens.EQEQ ->
                thenClause.isNotNullExpression() && elseClause.evaluatesTo(expression) &&
                !(thenClause is JetThrowExpression && thenClause.throwsNullPointerExceptionWithNoArguments())


            JetTokens.EXCLEQ ->
                elseClause.isNotNullExpression() && thenClause.evaluatesTo(expression) &&
                !(elseClause is JetThrowExpression && elseClause.throwsNullPointerExceptionWithNoArguments())

            else -> false
        }
    }

    private fun JetExpression.isNotNullExpression(): Boolean {
        val innerExpression = this.unwrapBlockOrParenthesis()
        return innerExpression !is JetBlockExpression && innerExpression.getNode().getElementType() != JetNodeTypes.NULL
    }

    override fun applyTo(element: JetIfExpression, editor: Editor) {
        val elvis = applyTo(element)
        elvis.inlineLeftSideIfApplicableWithPrompt(editor)
    }

    public fun applyTo(element: JetIfExpression): JetBinaryExpression {
        val condition = element.getCondition() as JetBinaryExpression

        val thenClause = element.getThen()!!
        val elseClause = element.getElse()!!
        val thenExpression = thenClause.unwrapBlockOrParenthesis()
        val elseExpression = elseClause.unwrapBlockOrParenthesis()

        val (left, right) =
                when(condition.getOperationToken()) {
                    JetTokens.EQEQ -> Pair(elseExpression, thenExpression)
                    JetTokens.EXCLEQ -> Pair(thenExpression, elseExpression)
                    else -> throw IllegalArgumentException()
                }

        val newExpr = element.replaced(JetPsiFactory(element).createExpressionByPattern("$0 ?: $1", left, right))
        return JetPsiUtil.deparenthesize(newExpr) as JetBinaryExpression
    }
}
