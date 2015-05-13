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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.intentions.calleeName
import org.jetbrains.kotlin.idea.intentions.toResolvedCall
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetDotQualifiedExpression
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.types.expressions.OperatorConventions

public class ReplaceCallWithBinaryOperatorIntention : JetSelfTargetingRangeIntention<JetDotQualifiedExpression>(javaClass(), "Replace call with binary operator") {
    override fun applicabilityRange(element: JetDotQualifiedExpression): TextRange? {
        val operation = operation(element.calleeName) ?: return null
        val resolvedCall = element.toResolvedCall() ?: return null
        if (!resolvedCall.getStatus().isSuccess()) return null
        if (resolvedCall.getCall().getTypeArgumentList() != null) return null
        val argument = resolvedCall.getCall().getValueArguments().singleOrNull() ?: return null
        if ((resolvedCall.getArgumentMapping(argument) as ArgumentMatch).valueParameter.getIndex() != 0) return null
        setText("Replace with '$operation' operator")
        return element.callExpression!!.getCalleeExpression()!!.getTextRange()
    }

    override fun applyTo(element: JetDotQualifiedExpression, editor: Editor) {
        val operation = operation(element.calleeName)!!
        val argument = element.callExpression!!.getValueArguments().single().getArgumentExpression()!!
        val receiver = element.getReceiverExpression()

        element.replace(JetPsiFactory(element).createExpressionByPattern("$0 $operation $1", receiver, argument))
    }

    private fun operation(functionName: String?): String? {
        if (functionName == null) return null
        return OperatorConventions.BINARY_OPERATION_NAMES.inverse()[Name.identifier(functionName)]?.getValue()
    }
}
