/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.intentions.branchedTransformations.intentions

import org.jetbrains.jet.plugin.intentions.JetSelfTargetingIntention
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.psi.JetIfExpression
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lang.psi.JetConstantExpression
import org.jetbrains.jet.lang.psi.JetBlockExpression
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.psi.JetPsiUtil

public class IfThenToElvisIntention : JetSelfTargetingIntention<JetIfExpression>("conditional.to.elvis", javaClass()) {

    private enum class ConditionalType {
        EQUALS_NULL
        NOT_EQUALS_NULL
    }

    fun getConditionalType (expression: JetBinaryExpression): ConditionalType? {
        val operationToken = expression.getOperationToken()

        val rightIsNull = expression.getRight()?.evaluatesToNull()
        val leftIsNull = expression.getLeft()?.evaluatesToNull()
        if (rightIsNull == null || leftIsNull == null || leftIsNull == rightIsNull)
            return null

        return when (operationToken) {
            JetTokens.EQEQ -> ConditionalType.EQUALS_NULL
            JetTokens.EXCLEQ -> ConditionalType.NOT_EQUALS_NULL
            else -> null
        }
    }


    override fun isApplicableTo(element: JetIfExpression): Boolean {
        val condition = element.getCondition()
        val thenClause = element.getThen()
        val elseClause = element.getElse()
        if (thenClause == null || elseClause == null || condition !is JetBinaryExpression) return false
        val expression = getNonNullExpression(condition)
        if (expression == null) return false
        val conditionalType = getConditionalType(condition)
        return when (conditionalType) {
            ConditionalType.NOT_EQUALS_NULL ->  thenClause.evaluatesTo(expression) && elseClause.evaluatesToNonNullExpression()
            ConditionalType.EQUALS_NULL ->  elseClause.evaluatesTo(expression) && thenClause.evaluatesToNonNullExpression()
            else -> false
        }
    }

    fun getNonNullExpression(condition: JetBinaryExpression): JetExpression? = when {
        condition.getLeft()?.evaluatesToNull() == false -> condition.getLeft()
        condition.getRight()?.evaluatesToNull() == false -> condition.getRight()
        else -> null
    }

    fun JetExpression.evaluatesTo(other: JetExpression) : Boolean {
        val expression = JetPsiUtil.deparenthesize(this)
        return when (expression) {
            is JetBlockExpression -> expression.getStatements().size() == 1 &&
            ((expression.getStatements().first!! as? JetExpression)?.evaluatesTo(other) ?: false)
            null -> false
            else -> expression.getText() == JetPsiUtil.deparenthesize(other)?.getText()
        }
    }

    fun getExpressionFromClause(clause: JetExpression): JetExpression? {
        val expression = JetPsiUtil.deparenthesize(clause)
        return when (expression) {
            is JetBlockExpression ->
                if (expression.getStatements().size() == 1){
                    getExpressionFromClause(expression.getStatements().first!! as JetExpression)
                }
                else null
            null -> null
            else -> expression
        }
    }


    fun JetExpression.evaluatesToNonNullExpression(): Boolean =
        (getExpressionFromClause(this)?.getText() ?: "null") != "null"

    fun JetExpression.evaluatesToNull(): Boolean {
        val expression = JetPsiUtil.deparenthesize(this)
        return when (expression) {
            is JetBlockExpression -> expression.getStatements().size() == 1 &&
            (expression.getStatements().first!! as? JetExpression)?.evaluatesToNull() ?: false
            is JetConstantExpression -> expression.getText() == "null"
            else -> false
        }
    }


    private data class Elvis(val lhs: JetExpression, val rhs: JetExpression)
    override fun applyTo(element: JetIfExpression, editor: Editor) {
        val condition = element.getCondition() as JetBinaryExpression

        val thenClause = checkNotNull(element.getThen(), "The then clause cannot be null")
        val elseClause = checkNotNull(element.getElse(), "The else clause cannot be null")
        val conditionType = checkNotNull(getConditionalType(condition), "The condition type cannot be null")
        val thenExpression = checkNotNull(getExpressionFromClause(thenClause), "Then clause must contain expression")
        val elseExpression = checkNotNull(getExpressionFromClause(elseClause), "Else clause must contain expression")

        val (lhs, rhs) = when(conditionType) {
            ConditionalType.EQUALS_NULL -> Elvis(elseExpression, thenExpression)
            ConditionalType.NOT_EQUALS_NULL -> Elvis(thenExpression, elseExpression)
        }

        val resultingExprString = "${lhs.getText()} ?: ${rhs.getText()}"
        element.replace(JetPsiFactory.createExpression(element.getProject(),resultingExprString))
    }


}
