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
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingOffsetIndependentIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.*
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*

public class IfThenToDoubleBangIntention : JetSelfTargetingOffsetIndependentIntention<JetIfExpression>(javaClass(), "Replace 'if' expression with '!!' expression") {
    override fun isApplicableTo(element: JetIfExpression): Boolean {
        val condition = element.getCondition() as? JetBinaryExpression ?: return false
        val thenClause = element.getThen() ?: return false
        val elseClause = element.getElse()

        val expression = condition.expressionComparedToNull() ?: return false

        val token = condition.getOperationToken()

        val throwExpression: JetThrowExpression
        val matchingClause: JetExpression?
        when (token) {
            JetTokens.EQEQ -> {
                throwExpression = thenClause.unwrapBlock() as? JetThrowExpression ?: return false
                matchingClause = elseClause
            }

            JetTokens.EXCLEQ -> {
                matchingClause = thenClause
                throwExpression = elseClause?.unwrapBlock() as? JetThrowExpression ?: return false
            }

            else -> throw IllegalStateException()
        }

        val matchesAsStatement = element.isStatement() && (matchingClause?.isNullExpressionOrEmptyBlock() ?: true)
        if (!matchesAsStatement && !(matchingClause?.evaluatesTo(expression) ?: false && expression.isStableVariable())) return false

        var text = "Replace 'if' expression with '!!' expression"
        if (!throwExpression.throwsNullPointerExceptionWithNoArguments()) {
            text += " (will remove exception)"
        }

        setText(text)
        return true
    }

    override fun applyTo(element: JetIfExpression, editor: Editor) {
        val condition = element.getCondition() as JetBinaryExpression
        val expression = condition.expressionComparedToNull()!!
        val result = element.replace(JetPsiFactory(element).createExpressionByPattern("$0!!", expression)) as JetPostfixExpression

        result.inlineBaseExpressionIfApplicableWithPrompt(editor)
    }
}
