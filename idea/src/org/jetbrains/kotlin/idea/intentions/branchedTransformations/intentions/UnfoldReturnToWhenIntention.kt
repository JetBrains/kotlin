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
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingOffsetIndependentIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.BranchedUnfoldingUtils
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.copied

public class UnfoldReturnToWhenIntention : JetSelfTargetingOffsetIndependentIntention<JetReturnExpression>(javaClass(), "Replace return with 'when' expression") {
    override fun isApplicableTo(element: JetReturnExpression): Boolean {
        val expr = element.getReturnedExpression()
        return expr is JetWhenExpression && JetPsiUtil.checkWhenExpressionHasSingleElse(expr)
    }

    override fun applyTo(element: JetReturnExpression, editor: Editor) {
        val whenExpression = element.getReturnedExpression() as JetWhenExpression
        val newWhenExpression = whenExpression.copied()

        for (entry in newWhenExpression.getEntries()) {
            val currExpr = BranchedUnfoldingUtils.getOutermostLastBlockElement(entry.getExpression())
            currExpr.replace(JetPsiFactory(element).createExpressionByPattern("return $0", currExpr))
        }

        element.replace(newWhenExpression)
    }
}
