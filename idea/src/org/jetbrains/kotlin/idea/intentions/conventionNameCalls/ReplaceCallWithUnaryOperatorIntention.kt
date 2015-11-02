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

package org.jetbrains.kotlin.idea.intentions.conventionNameCalls

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.intentions.calleeName
import org.jetbrains.kotlin.idea.intentions.isReceiverExpressionWithValue
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.types.expressions.OperatorConventions

public class ReplaceCallWithUnaryOperatorIntention : SelfTargetingRangeIntention<KtDotQualifiedExpression>(javaClass(), "Replace call with unary operator"), HighPriorityAction {
    override fun applicabilityRange(element: KtDotQualifiedExpression): TextRange? {
        val operation = operation(element.calleeName) ?: return null

        val call = element.callExpression ?: return null
        if (call.getTypeArgumentList() != null) return null
        if (!call.getValueArguments().isEmpty()) return null

        if (!element.isReceiverExpressionWithValue()) return null

        setText("Replace with '$operation' operator")
        return call.getCalleeExpression()!!.getTextRange()
    }

    override fun applyTo(element: KtDotQualifiedExpression, editor: Editor) {
        val operation = operation(element.calleeName)!!
        val receiver = element.getReceiverExpression()
        element.replace(KtPsiFactory(element).createExpressionByPattern("$0$1", operation, receiver))
    }

    private fun operation(functionName: String?) : String? {
        if (functionName == null) return null
        return OperatorConventions.UNARY_OPERATION_NAMES.inverse()[Name.identifier(functionName)]?.getValue()
    }
}
