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
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsStatement

class IfThenToDoubleBangIntention : SelfTargetingRangeIntention<KtIfExpression>(KtIfExpression::class.java, "Replace 'if' expression with '!!' expression") {
    override fun applicabilityRange(element: KtIfExpression): TextRange? {
        val condition = element.condition as? KtBinaryExpression ?: return null
        val thenClause = element.then ?: return null
        val elseClause = element.`else`

        val expression = condition.expressionComparedToNull() ?: return null

        val token = condition.operationToken

        val throwExpression: KtThrowExpression
        val matchingClause: KtExpression?
        when (token) {
            KtTokens.EQEQ -> {
                throwExpression = thenClause.unwrapBlockOrParenthesis() as? KtThrowExpression ?: return null
                matchingClause = elseClause
            }

            KtTokens.EXCLEQ -> {
                matchingClause = thenClause
                throwExpression = elseClause?.unwrapBlockOrParenthesis() as? KtThrowExpression ?: return null
            }

            else -> throw IllegalStateException()
        }

        val matchesAsStatement = element.isUsedAsStatement(element.analyze()) && (matchingClause?.isNullExpressionOrEmptyBlock() ?: true)
        if (!matchesAsStatement && !(matchingClause?.evaluatesTo(expression) ?: false && expression.isStableVariable())) return null

        var text = "Replace 'if' expression with '!!' expression"
        if (!throwExpression.throwsNullPointerExceptionWithNoArguments()) {
            text += " (will remove exception)"
        }

        setText(text)
        val rParen = element.rightParenthesis ?: return null
        return TextRange(element.startOffset, rParen.endOffset)
    }

    override fun applyTo(element: KtIfExpression, editor: Editor?) {
        val condition = element.condition as KtBinaryExpression
        val expression = condition.expressionComparedToNull()!!
        val result = element.replace(KtPsiFactory(element).createExpressionByPattern("$0!!", expression)) as KtPostfixExpression

        result.inlineBaseExpressionIfApplicableWithPrompt(editor)
    }
}
