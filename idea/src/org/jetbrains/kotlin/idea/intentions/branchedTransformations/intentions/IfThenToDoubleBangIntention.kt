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
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.psi.JetIfExpression
import org.jetbrains.kotlin.psi.JetPostfixExpression
import org.jetbrains.kotlin.psi.JetThrowExpression

public class IfThenToDoubleBangIntention : JetSelfTargetingOffsetIndependentIntention<JetIfExpression>("if.then.to.double.bang", javaClass()) {

    override fun isApplicableTo(element: JetIfExpression): Boolean {
        val condition = element.getCondition()
        val thenClause = element.getThen()
        val elseClause = element.getElse()

        if (condition !is JetBinaryExpression || !condition.comparesNonNullToNull()) return false

        val expression = condition.getNonNullExpression()

        if (expression == null) return false

        val token = condition.getOperationToken()
        if (token != JetTokens.EQEQ && token != JetTokens.EXCLEQ) return false

        val throwExpression =
                when (token) {
                    JetTokens.EQEQ -> thenClause?.unwrapBlock()
                    JetTokens.EXCLEQ -> elseClause?.unwrapBlock()
                    else -> throw IllegalStateException("Token must be either '!=' or '==' ")
                } as? JetThrowExpression ?: return false

        val matchingClause =
                when (token) {
                    JetTokens.EQEQ -> elseClause
                    JetTokens.EXCLEQ -> thenClause
                    else -> throw IllegalStateException("Token must be either '!=' or '==' ")
                }

        val matchesAsStatement = element.isStatement() && (matchingClause?.isNullExpressionOrEmptyBlock() ?: true)
        val matches = matchesAsStatement || (matchingClause?.evaluatesTo(expression) ?: false && expression.isStableVariable())

        if (matches) {
            val message =
                    if (throwExpression.throwsNullPointerExceptionWithNoArguments()) {
                        JetBundle.message("if.then.to.double.bang")
                    }
                    else {
                        // Warn that custom exception will be overwritten by intention action
                        JetBundle.message("if.then.to.double.bang.replace.exception")
                    }

            setText(message)
            return true
        }
        else {
            return false
        }
    }

    override fun applyTo(element: JetIfExpression, editor: Editor) {
        val condition = element.getCondition() as JetBinaryExpression

        val expression = checkNotNull(condition.getNonNullExpression(), "condition must contain non null expression")
        val resultingExprString = expression.getText() + "!!"
        val result = element.replace(resultingExprString) as JetPostfixExpression

        result.inlineBaseExpressionIfApplicableWithPrompt(editor)
    }
}
