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
import org.jetbrains.jet.lang.psi.JetIfExpression
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.plugin.intentions.branchedTransformations.evaluatesTo
import org.jetbrains.jet.plugin.intentions.branchedTransformations.comparesNonNullToNull
import org.jetbrains.jet.plugin.intentions.branchedTransformations.getNonNullExpression
import org.jetbrains.jet.plugin.intentions.branchedTransformations.replace
import org.jetbrains.jet.plugin.intentions.branchedTransformations.isStableVariable
import org.jetbrains.jet.lang.psi.JetPostfixExpression
import org.jetbrains.jet.plugin.intentions.branchedTransformations.inlineBaseExpressionIfApplicableWithPrompt
import org.jetbrains.jet.plugin.intentions.branchedTransformations.extractExpressionIfSingle
import org.jetbrains.jet.lang.psi.JetThrowExpression
import org.jetbrains.jet.plugin.JetBundle
import org.jetbrains.jet.plugin.intentions.branchedTransformations.isNullExpressionOrEmptyBlock
import org.jetbrains.jet.plugin.intentions.branchedTransformations.throwsNullPointerExceptionWithNoArguments
import org.jetbrains.jet.plugin.intentions.branchedTransformations.isStatement

public class IfThenToDoubleBangIntention : JetSelfTargetingIntention<JetIfExpression>("if.then.to.double.bang", javaClass()) {

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
                    JetTokens.EQEQ -> thenClause?.extractExpressionIfSingle()
                    JetTokens.EXCLEQ -> elseClause?.extractExpressionIfSingle()
                    else -> throw IllegalStateException("Token must be either '!=' or '==' ")
                } as? JetThrowExpression

        if (throwExpression == null) return false

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
