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
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.lastBlockStatementOrThis
import org.jetbrains.kotlin.psi.psiUtil.startOffset

public class UnfoldReturnToWhenIntention : SelfTargetingRangeIntention<KtReturnExpression>(javaClass(), "Replace return with 'when' expression"), LowPriorityAction {
    override fun applicabilityRange(element: KtReturnExpression): TextRange? {
        val whenExpr = element.getReturnedExpression() as? KtWhenExpression ?: return null
        if (!KtPsiUtil.checkWhenExpressionHasSingleElse(whenExpr)) return null
        if (whenExpr.getEntries().any { it.getExpression() == null }) return null
        return TextRange(element.startOffset, whenExpr.getWhenKeyword().endOffset)
    }

    override fun applyTo(element: KtReturnExpression, editor: Editor) {
        val whenExpression = element.getReturnedExpression() as KtWhenExpression
        val newWhenExpression = whenExpression.copied()

        for (entry in newWhenExpression.getEntries()) {
            val expr = entry.getExpression()!!.lastBlockStatementOrThis()
            expr.replace(KtPsiFactory(element).createExpressionByPattern("return $0", expr))
        }

        element.replace(newWhenExpression)
    }
}
