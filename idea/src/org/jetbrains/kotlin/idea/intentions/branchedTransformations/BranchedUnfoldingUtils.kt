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

package org.jetbrains.kotlin.idea.intentions.branchedTransformations

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.lastBlockStatementOrThis

object BranchedUnfoldingUtils {
    fun unfoldAssignmentToIf(assignment: KtBinaryExpression, editor: Editor?) {
        val op = assignment.operationReference.text
        val left = assignment.left!!
        val ifExpression = assignment.right as KtIfExpression

        val newIfExpression = ifExpression.copied()

        val thenExpr = newIfExpression.then!!.lastBlockStatementOrThis()
        val elseExpr = newIfExpression.`else`!!.lastBlockStatementOrThis()

        val psiFactory = KtPsiFactory(assignment)
        thenExpr.replace(psiFactory.createExpressionByPattern("$0 $1 $2", left, op, thenExpr))
        elseExpr.replace(psiFactory.createExpressionByPattern("$0 $1 $2", left, op, elseExpr))

        val resultIf = assignment.replace(newIfExpression)

        editor?.caretModel?.moveToOffset(resultIf.textOffset)
    }

    fun unfoldAssignmentToWhen(assignment: KtBinaryExpression, editor: Editor?) {
        val op = assignment.operationReference.text
        val left = assignment.left!!
        val whenExpression = assignment.right as KtWhenExpression

        val newWhenExpression = whenExpression.copied()

        for (entry in newWhenExpression.entries) {
            val expr = entry.expression!!.lastBlockStatementOrThis()
            expr.replace(KtPsiFactory(assignment).createExpressionByPattern("$0 $1 $2", left, op, expr))
        }

        val resultWhen = assignment.replace(newWhenExpression)

        editor?.caretModel?.moveToOffset(resultWhen.textOffset)
    }
}
