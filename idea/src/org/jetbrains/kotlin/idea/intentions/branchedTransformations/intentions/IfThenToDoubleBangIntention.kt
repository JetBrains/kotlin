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
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsStatement

class IfThenToDoubleBangIntention : SelfTargetingRangeIntention<KtIfExpression>(
        KtIfExpression::class.java, "Replace 'if' expression with '!!' expression"
) {
    override fun applicabilityRange(element: KtIfExpression): TextRange? {
        val (context, condition, receiverExpression, baseClause, negatedClause) = element.buildSelectTransformationData() ?: return null
        // TODO: here "Replace with as" can be supported
        if (condition is KtIsExpression) return null

        val throwExpression = negatedClause as? KtThrowExpression ?: return null

        val matchesAsStatement = element.isUsedAsStatement(context) && (baseClause?.isNullExpressionOrEmptyBlock() ?: true)
        if (!matchesAsStatement &&
            !(baseClause?.evaluatesTo(receiverExpression) ?: false && receiverExpression.isStableVariable())) return null

        var text = "Replace 'if' expression with '!!' expression"
        if (!throwExpression.throwsNullPointerExceptionWithNoArguments()) {
            text += " (will remove exception)"
        }

        setText(text)
        val rParen = element.rightParenthesis ?: return null
        return TextRange(element.startOffset, rParen.endOffset)
    }

    override fun applyTo(element: KtIfExpression, editor: Editor?) {
        val (_, _, receiverExpression) = element.buildSelectTransformationData() ?: return
        val result = element.replace(KtPsiFactory(element).createExpressionByPattern("$0!!", receiverExpression)) as KtPostfixExpression

        result.inlineBaseExpressionIfApplicableWithPrompt(editor)
    }
}
