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
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingRangeIntention
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.copied
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.lastBlockStatementOrThis
import org.jetbrains.kotlin.psi.psiUtil.startOffset

public class UnfoldReturnToIfIntention : JetSelfTargetingRangeIntention<JetReturnExpression>(javaClass(), "Replace return with 'if' expression") {
    override fun applicabilityRange(element: JetReturnExpression): TextRange? {
        val ifExpression = element.getReturnedExpression() as? JetIfExpression ?: return null
        return TextRange(element.startOffset, ifExpression.getIfKeyword().endOffset)
    }

    override fun applyTo(element: JetReturnExpression, editor: Editor) {
        val ifExpression = element.getReturnedExpression() as JetIfExpression
        val newIfExpression = ifExpression.copied()
        val thenExpr = newIfExpression.getThen()!!.lastBlockStatementOrThis()
        val elseExpr = newIfExpression.getElse()!!.lastBlockStatementOrThis()

        val psiFactory = JetPsiFactory(element)
        thenExpr.replace(psiFactory.createExpressionByPattern("return $0", thenExpr))
        elseExpr.replace(psiFactory.createExpressionByPattern("return $0", elseExpr))

        element.replace(newIfExpression)
    }
}