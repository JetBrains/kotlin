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
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.intentions.isReceiverExpressionWithValue
import org.jetbrains.kotlin.idea.intentions.toResolvedCall
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.buildExpression
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.util.OperatorNameConventions

public class ReplaceGetOrSetInspection : IntentionBasedInspection<KtDotQualifiedExpression>(
        ReplaceGetOrSetIntention(), ReplaceGetOrSetInspection.additionalChecker

) {
    companion object {
        val additionalChecker = { expression: KtDotQualifiedExpression ->
            (expression.toResolvedCall(BodyResolveMode.PARTIAL)!!.resultingDescriptor as FunctionDescriptor).isExplicitOperator()
        }
    }
}

private fun FunctionDescriptor.isExplicitOperator(): Boolean {
    return if (overriddenDescriptors.isEmpty())
        containingDeclaration !is JavaClassDescriptor && isOperator
    else
        overriddenDescriptors.any { it.isExplicitOperator() }
}

class ReplaceGetOrSetIntention : JetSelfTargetingRangeIntention<KtDotQualifiedExpression>(
        KtDotQualifiedExpression::class.java,
        "Replace 'get' or 'set' call with indexing operator"
), HighPriorityAction {

    private val operatorNames = setOf(OperatorNameConventions.GET, OperatorNameConventions.SET)

    override fun applicabilityRange(element: KtDotQualifiedExpression): TextRange? {
        val callExpression = element.callExpression ?: return null
        val bindingContext = callExpression.analyze(BodyResolveMode.PARTIAL)
        val resolvedCall = callExpression.getResolvedCall(bindingContext) ?: return null
        if (!resolvedCall.isReallySuccess()) return null

        val target = resolvedCall.resultingDescriptor as? FunctionDescriptor ?: return null
        if (!target.isOperator || target.name !in operatorNames) return null

        if (callExpression.getTypeArgumentList() != null) return null

        val arguments = callExpression.getValueArguments()
        if (arguments.isEmpty()) return null
        if (arguments.any { it.isNamed() }) return null

        if (!element.isReceiverExpressionWithValue()) return null

        if (target.name == OperatorNameConventions.SET && element.isUsedAsExpression(bindingContext)) return null

        text = "Replace '${target.name.asString()}' call with indexing operator"

        return callExpression.getCalleeExpression()!!.getTextRange()
    }

    override fun applyTo(element: KtDotQualifiedExpression, editor: Editor) {
        applyTo(element)
    }

    fun applyTo(element: KtDotQualifiedExpression) {
        val isSet = element.toResolvedCall(BodyResolveMode.PARTIAL)!!.resultingDescriptor.name == OperatorNameConventions.SET
        val allArguments = element.callExpression!!.valueArguments
        if (isSet) {
            assert(allArguments.size > 0)
        }

        val expression = KtPsiFactory(element).buildExpression {
            appendExpression(element.getReceiverExpression())

            appendFixedText("[")

            val arguments = if (isSet) allArguments.dropLast(1) else allArguments
            appendExpressions(arguments.map { it.getArgumentExpression() })

            appendFixedText("]")

            if (isSet) {
                appendFixedText("=")
                appendExpression(allArguments.last().getArgumentExpression())
            }
        }

        element.replace(expression)
    }
}
