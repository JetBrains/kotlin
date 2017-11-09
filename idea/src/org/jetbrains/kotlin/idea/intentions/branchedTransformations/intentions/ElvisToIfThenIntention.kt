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
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.convertToIfNotNullExpression
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.introduceValueForCondition
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isStable
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtPsiUtil

class ElvisToIfThenIntention : SelfTargetingRangeIntention<KtBinaryExpression>(KtBinaryExpression::class.java, "Replace elvis expression with 'if' expression"), LowPriorityAction {
    override fun applicabilityRange(element: KtBinaryExpression): TextRange? {
        return if (element.operationToken == KtTokens.ELVIS && element.left != null && element.right != null)
            element.operationReference.textRange
        else
            null
    }

    override fun applyTo(element: KtBinaryExpression, editor: Editor?) {
        val left = KtPsiUtil.safeDeparenthesize(element.left!!)
        val right = KtPsiUtil.safeDeparenthesize(element.right!!)

        val leftIsStable = left.isStable()

        val ifStatement = element.convertToIfNotNullExpression(left, left, right)

        if (!leftIsStable) {
            ifStatement.introduceValueForCondition(ifStatement.then!!, editor)
        }
    }

}
