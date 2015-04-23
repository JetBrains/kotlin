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
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingOffsetIndependentIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.*
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*

public class IfThenToElvisIntention : JetSelfTargetingOffsetIndependentIntention<JetIfExpression>("if.then.to.elvis", javaClass()) {

    override fun isApplicableTo(element: JetIfExpression): Boolean {
        val condition = element.getCondition()
        val thenClause = element.getThen()
        val elseClause = element.getElse()
        if (thenClause == null || elseClause == null || condition !is JetBinaryExpression || !condition.comparesNonNullToNull()) return false

        val expression = condition.getNonNullExpression()
        if (expression == null || !expression.isStableVariable()) return false

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

    override fun applyTo(element: JetIfExpression, editor: Editor) {
        val elvis = applyTo(element)
        elvis.inlineLeftSideIfApplicableWithPrompt(editor)
    }

    public fun applyTo(element: JetIfExpression): JetBinaryExpression {
        val condition = element.getCondition() as JetBinaryExpression

        val thenClause = checkNotNull(element.getThen(), "The then clause cannot be null")
        val elseClause = checkNotNull(element.getElse(), "The else clause cannot be null")
        val thenExpression = checkNotNull(thenClause.extractExpressionIfSingle(), "Then clause must contain expression")
        val elseExpression = checkNotNull(elseClause.extractExpressionIfSingle(), "Else clause must contain expression")

        val (left, right) =
                when(condition.getOperationToken()) {
                    JetTokens.EQEQ -> Pair(elseExpression, thenExpression)
                    JetTokens.EXCLEQ -> Pair(thenExpression, elseExpression)
                    else -> throw IllegalStateException("Operation token must be either null or not null")
                }

        val resultingExprString = "${left.getText()} ?: ${right.getText()}"
        val resultingExpression = JetPsiUtil.deparenthesize(element.replace(resultingExprString) as? JetExpression)

        assert(resultingExpression is JetBinaryExpression) {
               "Unexpected expression type: ${resultingExpression?.javaClass}, expected JetBinaryExpression, element = '${element.getText()}'"
        }

        return resultingExpression as JetBinaryExpression
    }
}
