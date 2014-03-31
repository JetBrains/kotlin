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

package org.jetbrains.jet.plugin.intentions

import org.jetbrains.jet.lang.psi.JetIfExpression
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.psi.psiUtil.getParentByType
import org.jetbrains.jet.lang.psi.JetExpression
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lexer.JetToken
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression

public class SplitIfIntention : JetSelfTargetingIntention<JetSimpleNameExpression>("split.if", javaClass()) {

    override fun isApplicableTo(element: JetSimpleNameExpression): Boolean {
        val operator = element.getReferencedNameElementType()
        if (operator != JetTokens.ANDAND && operator != JetTokens.OROR) return false

        if (element.getParent() !is JetBinaryExpression) return false
        var expression = element.getParent() as JetBinaryExpression

        if (expression.getRight() == null || expression.getLeft() == null) return false

        while (expression.getParent() is JetBinaryExpression) {
            expression = expression.getParent() as JetBinaryExpression
            if (operator == JetTokens.ANDAND && expression.getOperationToken() != JetTokens.ANDAND) return false
            if (operator == JetTokens.OROR && expression.getOperationToken() != JetTokens.OROR) return false
        }

        if (expression.getParent()?.getParent() !is JetIfExpression) return false
        val ifExpression = expression.getParent()?.getParent() as JetIfExpression

        if (ifExpression.getCondition() == null) return false
        if(!PsiTreeUtil.isAncestor(ifExpression.getCondition(), element, false)) return false

        if (ifExpression.getThen() == null) return false

        return true
    }

    override fun applyTo(element: JetSimpleNameExpression, editor: Editor) {
        val ifExpression = element.getParentByType(javaClass<JetIfExpression>())
        val expression = element.getParent() as JetBinaryExpression
        val rightExpression = getRight(expression, ifExpression!!.getCondition() as JetExpression)
        val leftExpression = expression.getLeft()
        val elseExpression = ifExpression.getElse()
        val thenExpression = ifExpression.getThen()

        if (element.getReferencedNameElementType() == JetTokens.ANDAND) {
            ifExpression.replace(JetPsiFactory.createIf(element.getProject(), leftExpression,
                JetPsiFactory.wrapInABlock(JetPsiFactory.createIf(element.getProject(), rightExpression, thenExpression,
                elseExpression)), elseExpression))
        } else {
            ifExpression.replace(JetPsiFactory.createIf(element.getProject(), leftExpression, thenExpression,
                JetPsiFactory.createIf(element.getProject(), rightExpression, thenExpression, elseExpression)))
        }
    }

    fun getRight(element: JetBinaryExpression, condition: JetExpression): JetExpression {
        //gets the textOffset of the right side of the JetBinaryExpression in context to condition
        val startOffset = element.getRight()!!.getTextOffset() - condition.getTextOffset()
        val rightString = condition.getText()!![startOffset, condition.getTextLength()].toString()

        return JetPsiFactory.createExpression(element.getProject(), rightString)
    }
}

