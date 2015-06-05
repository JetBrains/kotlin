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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall

public class ConvertAssertToIfWithThrowIntention : JetSelfTargetingIntention<JetCallExpression>(javaClass(), "Replace 'assert' with 'if' statement"), LowPriorityAction {
    override fun isApplicableTo(element: JetCallExpression, caretOffset: Int): Boolean {
        val callee = element.getCalleeExpression() ?: return false
        if (!callee.getTextRange().containsOffset(caretOffset)) return false

        val argumentSize = element.getValueArguments().size()
        if (argumentSize !in 1..2) return false
        val functionLiterals = element.getFunctionLiteralArguments()
        if (functionLiterals.size() > 1) return false
        if (functionLiterals.size() == 1 && argumentSize == 1) return false // "assert {...}" is incorrect

        val resolvedCall = element.getResolvedCall(element.analyze()) ?: return false
        return DescriptorUtils.getFqName(resolvedCall.getResultingDescriptor()).asString() == "kotlin.assert"
    }

    override fun applyTo(element: JetCallExpression, editor: Editor) {
        val args = element.getValueArguments()
        val conditionText = args[0]?.getArgumentExpression()?.getText() ?: return
        val functionLiteral = element.getFunctionLiteralArguments().singleOrNull()
        val messageIsFunction = messageIsFunction(element)

        val psiFactory = JetPsiFactory(element)

        val messageExpr = when {
            args.size() == 2 -> args[1]?.getArgumentExpression() ?: return
            functionLiteral != null -> functionLiteral!!
            else -> psiFactory.createExpression("\"Assertion failed\"")
        }

        val ifExpression = replaceWithIfThenThrowExpression(element)

        // shorten java.lang.AssertionError
        ShortenReferences.DEFAULT.process(ifExpression.getThen()!!)

        val ifCondition = ifExpression.getCondition() as JetPrefixExpression
        ifCondition.getBaseExpression()!!.replace(psiFactory.createExpression(conditionText))

        val thrownExpression = ((ifExpression.getThen() as JetBlockExpression).getStatements().single() as JetThrowExpression).getThrownExpression()
        val assertionErrorCall = if (thrownExpression is JetCallExpression)
            thrownExpression
        else
            (thrownExpression as JetDotQualifiedExpression).getSelectorExpression() as JetCallExpression

        val message = psiFactory.createExpression(
                if (messageIsFunction && messageExpr is JetCallableReferenceExpression) {
                    messageExpr.getCallableReference().getText() + "()"
                }
                else if (messageIsFunction) {
                    messageExpr.getText() + "()"
                }
                else {
                    messageExpr.getText()
                }
        )
        assertionErrorCall.getValueArguments().single().getArgumentExpression()!!.replace(message)

        simplifyConditionIfPossible(ifExpression)
    }

    private fun messageIsFunction(callExpr: JetCallExpression): Boolean {
        val resolvedCall = callExpr.getResolvedCall(callExpr.analyze()) ?: return false
        val valParameters = resolvedCall.getResultingDescriptor().getValueParameters()
        return valParameters.size() > 1 && valParameters[1].getType() != KotlinBuiltIns.getInstance().getAnyType()
    }

    private fun simplifyConditionIfPossible(ifExpression: JetIfExpression) {
        val condition = ifExpression.getCondition() as JetPrefixExpression
        val simplifier = SimplifyNegatedBinaryExpressionIntention()
        if (simplifier.isApplicableTo(condition)) {
            simplifier.applyTo(condition)
        }
    }

    private fun replaceWithIfThenThrowExpression(original: JetCallExpression): JetIfExpression {
        val replacement = JetPsiFactory(original).createExpression("if (!true) { throw java.lang.AssertionError(\"\") }") as JetIfExpression
        val parent = original.getParent()
        return if (parent is JetDotQualifiedExpression)
            parent.replaced(replacement)
        else
            original.replaced(replacement)
    }
}
