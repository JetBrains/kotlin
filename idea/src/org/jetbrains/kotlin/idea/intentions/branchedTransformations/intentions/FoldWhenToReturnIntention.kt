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
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.BranchedFoldingUtils
import org.jetbrains.kotlin.psi.*

public class FoldWhenToReturnIntention : SelfTargetingRangeIntention<KtWhenExpression>(javaClass(), "Replace 'when' expression with return") {
    override fun applicabilityRange(element: KtWhenExpression): TextRange? {
        if (!KtPsiUtil.checkWhenExpressionHasSingleElse(element)) return null

        val entries = element.getEntries()

        if (entries.isEmpty()) return null

        for (entry in entries) {
            if (BranchedFoldingUtils.getFoldableBranchedReturn(entry.getExpression()) == null) return null
        }

        return element.getWhenKeyword().getTextRange()
    }

    override fun applyTo(element: KtWhenExpression, editor: Editor) {
        assert(!element.getEntries().isEmpty())

        val newReturnExpression = KtPsiFactory(element).createExpressionByPattern("return $0", element) as KtReturnExpression
        val newWhenExpression = newReturnExpression.getReturnedExpression() as KtWhenExpression

        for (entry in newWhenExpression.getEntries()) {
            val currReturn = BranchedFoldingUtils.getFoldableBranchedReturn(entry.getExpression()!!)!!
            val currExpr = currReturn.getReturnedExpression()!!
            currReturn.replace(currExpr)
        }

        element.replace(newReturnExpression)
    }
}