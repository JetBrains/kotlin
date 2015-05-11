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
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.copied

public object BranchedUnfoldingUtils {
    public fun getOutermostLastBlockElement(expression: JetExpression?): JetExpression {
        return JetPsiUtil.getOutermostLastBlockElement(expression, JetPsiUtil.ANY_JET_ELEMENT) as JetExpression
    }

    public fun unfoldAssignmentToIf(assignment: JetBinaryExpression, editor: Editor) {
        val op = assignment.getOperationReference().getText()
        val left = assignment.getLeft()!!
        val ifExpression = assignment.getRight() as JetIfExpression

        val newIfExpression = ifExpression.copied()

        val thenExpr = getOutermostLastBlockElement(newIfExpression.getThen())
        val elseExpr = getOutermostLastBlockElement(newIfExpression.getElse())

        val psiFactory = JetPsiFactory(assignment)
        thenExpr.replace(psiFactory.createExpressionByPattern("$0 $1 $2", left, op, thenExpr))
        elseExpr.replace(psiFactory.createExpressionByPattern("$0 $1 $2", left, op, elseExpr))

        val resultIf = assignment.replace(newIfExpression)

        editor.getCaretModel().moveToOffset(resultIf.getTextOffset())
    }

    public fun unfoldAssignmentToWhen(assignment: JetBinaryExpression, editor: Editor) {
        val op = assignment.getOperationReference().getText()
        val left = assignment.getLeft()!!
        val whenExpression = assignment.getRight() as JetWhenExpression

        val newWhenExpression = whenExpression.copied()

        for (entry in newWhenExpression.getEntries()) {
            val expr = getOutermostLastBlockElement(entry.getExpression())
            expr.replace(JetPsiFactory(assignment).createExpressionByPattern("$0 $1 $2", left, op, expr))
        }

        val resultWhen = assignment.replace(newWhenExpression)

        editor.getCaretModel().moveToOffset(resultWhen.getTextOffset())
    }
}
