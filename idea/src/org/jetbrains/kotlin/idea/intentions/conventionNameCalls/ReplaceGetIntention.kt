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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.intentions.isReceiverExpressionWithValue
import org.jetbrains.kotlin.idea.intentions.toResolvedCall
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.buildExpression
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess

public class ExplicitGetInspection : IntentionBasedInspection<KtDotQualifiedExpression>(
        ReplaceGetIntention(), ExplicitGetInspection.additionalChecker

) {
    companion object {
        val additionalChecker = { expression: KtDotQualifiedExpression ->
            (expression.toResolvedCall()!!.resultingDescriptor as FunctionDescriptor).isExplicitOperator()
        }
    }
}

private fun FunctionDescriptor.isExplicitOperator(): Boolean {
    return if (overriddenDescriptors.isEmpty())
        containingDeclaration !is JavaClassDescriptor && isOperator
    else
        overriddenDescriptors.any { it.isExplicitOperator() }
}

public class ReplaceGetIntention : JetSelfTargetingRangeIntention<KtDotQualifiedExpression>(javaClass(), "Replace 'get' call with index operator"), HighPriorityAction {
    override fun applicabilityRange(element: KtDotQualifiedExpression): TextRange? {
        val resolvedCall = element.toResolvedCall() ?: return null
        if (!resolvedCall.isReallySuccess()) return null
        val target = resolvedCall.resultingDescriptor as? FunctionDescriptor ?: return null
        if (target.name.asString() != "get" || !target.isOperator) return null

        val call = element.callExpression ?: return null
        if (call.getTypeArgumentList() != null) return null

        val arguments = call.getValueArguments()
        if (arguments.isEmpty()) return null
        if (arguments.any { it.isNamed() }) return null

        if (!element.isReceiverExpressionWithValue()) return null

        return call.getCalleeExpression()!!.getTextRange()
    }

    override fun applyTo(element: KtDotQualifiedExpression, editor: Editor) {
        applyTo(element)
    }

    fun applyTo(element: KtDotQualifiedExpression) {
        val expression = KtPsiFactory(element).buildExpression {
            appendExpression(element.getReceiverExpression())

            appendFixedText("[")

            val call = element.callExpression!!
            for ((index, argument) in call.getValueArguments().withIndex()) {
                if (index > 0) {
                    appendFixedText(",")
                }
                appendExpression(argument.getArgumentExpression())
            }

            appendFixedText("]")
        }
        element.replace(expression)
    }
}
