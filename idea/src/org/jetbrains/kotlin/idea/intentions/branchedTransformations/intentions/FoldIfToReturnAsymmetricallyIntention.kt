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

class FoldIfToReturnAsymmetricallyIntention : SelfTargetingRangeIntention<KtIfExpression>(KtIfExpression::class.java, "Replace 'if' expression with return") {
    override fun applicabilityRange(element: KtIfExpression): TextRange? {
        if (BranchedFoldingUtils.getFoldableBranchedReturn(element.then) == null || element.`else` != null) {
            return null
        }

        val nextElement = KtPsiUtil.skipTrailingWhitespacesAndComments(element) as? KtReturnExpression
        if (nextElement?.returnedExpression == null) return null
        return element.ifKeyword.textRange
    }

    override fun applyTo(element: KtIfExpression, editor: Editor?) {
        val condition = element.condition!!
        val thenBranch = element.then!!
        val elseBranch = KtPsiUtil.skipTrailingWhitespacesAndComments(element) as KtReturnExpression

        val psiFactory = KtPsiFactory(element)
        val newIfExpression = psiFactory.createIf(condition, thenBranch, elseBranch)

        val thenReturn = BranchedFoldingUtils.getFoldableBranchedReturn(newIfExpression.then!!)!!
        val elseReturn = BranchedFoldingUtils.getFoldableBranchedReturn(newIfExpression.`else`!!)!!

        thenReturn.replace(thenReturn.returnedExpression!!)
        elseReturn.replace(elseReturn.returnedExpression!!)

        element.replace(psiFactory.createExpressionByPattern("return $0", newIfExpression))
        elseBranch.delete()
    }
}