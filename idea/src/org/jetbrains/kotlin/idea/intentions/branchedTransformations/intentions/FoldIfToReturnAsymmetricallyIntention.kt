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
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.BranchedFoldingUtils
import org.jetbrains.kotlin.psi.*

public class FoldIfToReturnAsymmetricallyIntention : JetSelfTargetingRangeIntention<JetIfExpression>(javaClass(), "Replace 'if' expression with return") {
    override fun applicabilityRange(element: JetIfExpression): TextRange? {
        if (BranchedFoldingUtils.getFoldableBranchedReturn(element.getThen()) == null || element.getElse() != null) {
            return null
        }

        val nextElement = JetPsiUtil.skipTrailingWhitespacesAndComments(element) as? JetReturnExpression
        if (nextElement?.getReturnedExpression() == null) return null
        return element.getIfKeyword().getTextRange()
    }

    override fun applyTo(element: JetIfExpression, editor: Editor) {
        val condition = element.getCondition()!!
        val thenBranch = element.getThen()!!
        val elseBranch = JetPsiUtil.skipTrailingWhitespacesAndComments(element) as JetReturnExpression

        val psiFactory = JetPsiFactory(element)
        var newIfExpression = psiFactory.createIf(condition, thenBranch, elseBranch)

        val thenReturn = BranchedFoldingUtils.getFoldableBranchedReturn(newIfExpression.getThen()!!)!!
        val elseReturn = BranchedFoldingUtils.getFoldableBranchedReturn(newIfExpression.getElse()!!)!!

        thenReturn.replace(thenReturn.getReturnedExpression()!!)
        elseReturn.replace(elseReturn.getReturnedExpression()!!)

        element.replace(psiFactory.createExpressionByPattern("return $0", newIfExpression))
        elseBranch.delete()
    }
}