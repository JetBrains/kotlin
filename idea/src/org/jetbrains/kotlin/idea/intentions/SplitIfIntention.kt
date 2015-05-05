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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.*

public class SplitIfIntention : JetSelfTargetingIntention<JetExpression>(javaClass(), "Split if into 2 if's") {
    override fun isApplicableTo(element: JetExpression, caretOffset: Int): Boolean {
        return when (element) {
            is JetSimpleNameExpression -> isOperatorValid(element)
            is JetIfExpression -> getFirstValidOperator(element) != null && element.getIfKeyword().getTextRange().containsOffset(caretOffset)
            else -> false
        }
    }

    override fun applyTo(element: JetExpression, editor: Editor) {
        val operator = when (element) {
            is JetIfExpression -> getFirstValidOperator(element)!!
            else -> element as JetSimpleNameExpression
        }

        val ifExpression = operator.getNonStrictParentOfType<JetIfExpression>()
        val expression = operator.getParent() as JetBinaryExpression
        val rightExpression = JetPsiUtil.safeDeparenthesize(getRight(expression, ifExpression!!.getCondition()!!))
        val leftExpression = JetPsiUtil.safeDeparenthesize(expression.getLeft()!!)
        val thenExpression = ifExpression.getThen()!!
        val elseExpression = ifExpression.getElse()

        val psiFactory = JetPsiFactory(element)
        val innerIf = psiFactory.createIf(rightExpression, thenExpression, elseExpression)
        val newIf = when (operator.getReferencedNameElementType()) {
            JetTokens.ANDAND -> psiFactory.createIf(leftExpression, psiFactory.wrapInABlock(innerIf), elseExpression)

            JetTokens.OROR -> psiFactory.createIf(leftExpression, thenExpression, innerIf)

            else -> throw IllegalArgumentException()
        }
        ifExpression.replace(newIf)
    }

    private fun getRight(element: JetBinaryExpression, condition: JetExpression): JetExpression {
        //gets the textOffset of the right side of the JetBinaryExpression in context to condition
        val startOffset = element.getRight()!!.getTextOffset() - condition.getTextOffset()
        val rightString = condition.getText()!!.substring(startOffset, condition.getTextLength())

        return JetPsiFactory(element).createExpression(rightString)
    }

    private fun getFirstValidOperator(element: JetIfExpression): JetSimpleNameExpression? {
        val condition = element.getCondition() ?: return null
        return PsiTreeUtil.findChildrenOfType(condition, javaClass<JetSimpleNameExpression>())
                .firstOrNull { isOperatorValid(it) }
    }

    private fun isOperatorValid(element: JetSimpleNameExpression): Boolean {
        val operator = element.getReferencedNameElementType()
        if (operator != JetTokens.ANDAND && operator != JetTokens.OROR) return false

        var expression = element.getParent() as? JetBinaryExpression ?: return false

        if (expression.getRight() == null || expression.getLeft() == null) return false

        while (true) {
            expression = expression.getParent() as? JetBinaryExpression ?: break
            if (expression.getOperationToken() != operator) return false
        }

        val ifExpression = expression.getParent()?.getParent() as? JetIfExpression ?: return false

        if (ifExpression.getCondition() == null) return false
        if (!PsiTreeUtil.isAncestor(ifExpression.getCondition(), element, false)) return false

        if (ifExpression.getThen() == null) return false

        return true
    }
}

