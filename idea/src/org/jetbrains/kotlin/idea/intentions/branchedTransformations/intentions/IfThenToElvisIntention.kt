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
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.intentions.SelfTargetingOffsetIndependentIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class IfThenToElvisInspection : IntentionBasedInspection<KtIfExpression>(IfThenToElvisIntention::class)

class IfThenToElvisIntention : SelfTargetingOffsetIndependentIntention<KtIfExpression>(KtIfExpression::class.java, "Replace 'if' expression with elvis expression") {

    override fun isApplicableTo(element: KtIfExpression): Boolean {
        val condition = element.condition as? KtBinaryExpression ?: return false
        val thenClause = element.then ?: return false
        val elseClause = element.`else` ?: return false

        val expression = condition.expressionComparedToNull() ?: return false
        if (!expression.isStableVariable()) return false

        return when (condition.operationToken) {
            KtTokens.EQEQ ->
                thenClause.isNotNullExpression() && elseClause.evaluatesTo(expression) &&
                !(thenClause is KtThrowExpression && thenClause.throwsNullPointerExceptionWithNoArguments())


            KtTokens.EXCLEQ ->
                elseClause.isNotNullExpression() && thenClause.evaluatesTo(expression) &&
                !(elseClause is KtThrowExpression && elseClause.throwsNullPointerExceptionWithNoArguments())

            else -> false
        }
    }

    private fun KtExpression.isNotNullExpression(): Boolean {
        val innerExpression = this.unwrapBlockOrParenthesis()
        return innerExpression !is KtBlockExpression && innerExpression.node.elementType != KtNodeTypes.NULL
    }

    override fun applyTo(element: KtIfExpression, editor: Editor?) {
        val condition = element.condition as KtBinaryExpression

        val thenClause = element.then!!
        val elseClause = element.`else`!!
        val thenExpression = thenClause.unwrapBlockOrParenthesis()
        val elseExpression = elseClause.unwrapBlockOrParenthesis()

        val (left, right) =
                when(condition.operationToken) {
                    KtTokens.EQEQ -> Pair(elseExpression, thenExpression)
                    KtTokens.EXCLEQ -> Pair(thenExpression, elseExpression)
                    else -> throw IllegalArgumentException()
                }

        val newExpr = element.replaced(KtPsiFactory(element).createExpressionByPattern("$0 ?: $1", left, right))
        val elvis = KtPsiUtil.deparenthesize(newExpr) as KtBinaryExpression

        if (editor != null) {
            elvis.inlineLeftSideIfApplicableWithPrompt(editor)
        }
    }
}
