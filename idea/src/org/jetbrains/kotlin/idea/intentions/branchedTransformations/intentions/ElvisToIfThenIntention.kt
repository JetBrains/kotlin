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
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.convertToIfNotNullExpression
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.introduceValueForCondition
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isStableVariable
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.psi.JetPsiUtil

public class ElvisToIfThenIntention : JetSelfTargetingRangeIntention<JetBinaryExpression>(javaClass(), "Replace elvis expression with 'if' expression") {
    override fun applicabilityRange(element: JetBinaryExpression): TextRange? {
        return if (element.getOperationToken() == JetTokens.ELVIS && element.getLeft() != null && element.getRight() != null)
            element.getOperationReference().getTextRange()
        else
            null
    }

    override fun applyTo(element: JetBinaryExpression, editor: Editor) {
        val left = JetPsiUtil.safeDeparenthesize(element.getLeft()!!)
        val right = JetPsiUtil.safeDeparenthesize(element.getRight()!!)

        val leftIsStable = left.isStableVariable()

        val ifStatement = element.convertToIfNotNullExpression(left, left, right)

        if (!leftIsStable) {
            ifStatement.introduceValueForCondition(ifStatement.getThen()!!, editor)
        }
    }

}
