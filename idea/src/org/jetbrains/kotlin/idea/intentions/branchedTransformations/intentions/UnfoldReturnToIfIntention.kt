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

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.lastBlockStatementOrThis
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class UnfoldReturnToIfIntention : SelfTargetingRangeIntention<KtReturnExpression>(KtReturnExpression::class.java, "Replace return with 'if' expression"), LowPriorityAction {
    override fun applicabilityRange(element: KtReturnExpression): TextRange? {
        val ifExpression = element.returnedExpression as? KtIfExpression ?: return null
        return TextRange(element.startOffset, ifExpression.ifKeyword.endOffset)
    }

    override fun applyTo(element: KtReturnExpression, editor: Editor?) {
        val ifExpression = element.returnedExpression as KtIfExpression
        val newIfExpression = ifExpression.copied()
        val thenExpr = newIfExpression.then!!.lastBlockStatementOrThis()
        val elseExpr = newIfExpression.`else`!!.lastBlockStatementOrThis()

        val psiFactory = KtPsiFactory(element)
        thenExpr.replace(psiFactory.createExpressionByPattern("return $0", thenExpr))
        elseExpr.replace(psiFactory.createExpressionByPattern("return $0", elseExpr))

        element.replace(newIfExpression)
    }
}