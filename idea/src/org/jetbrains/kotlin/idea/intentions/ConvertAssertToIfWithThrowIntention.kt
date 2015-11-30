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
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.addToStdlib.check

public class ConvertAssertToIfWithThrowIntention : SelfTargetingIntention<KtCallExpression>(javaClass(), "Replace 'assert' with 'if' statement"), LowPriorityAction {
    override fun isApplicableTo(element: KtCallExpression, caretOffset: Int): Boolean {
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

    override fun applyTo(element: KtCallExpression, editor: Editor) {
        val args = element.getValueArguments()
        val conditionText = args[0]?.getArgumentExpression()?.getText() ?: return
        val functionLiteralArgument = element.functionLiteralArguments.singleOrNull()
        val bindingContext = element.analyze(BodyResolveMode.PARTIAL)
        val psiFactory = KtPsiFactory(element)

        val messageFunctionExpr = when {
            args.size == 2 -> args[1]?.getArgumentExpression() ?: return
            functionLiteralArgument != null -> functionLiteralArgument.getFunctionLiteral()
            else -> null
        }

        val extractedMessageSingleExpr = (messageFunctionExpr as? KtFunctionLiteralExpression)?.let { extractMessageSingleExpression(it, bindingContext) }

        val messageIsFunction = extractedMessageSingleExpr == null && messageIsFunction(element, bindingContext)
        val messageExpr = extractedMessageSingleExpr ?: messageFunctionExpr ?: psiFactory.createExpression("\"Assertion failed\"")

        val ifExpression = replaceWithIfThenThrowExpression(element)

        // shorten java.lang.AssertionError
        ShortenReferences.DEFAULT.process(ifExpression.getThen()!!)

        val ifCondition = ifExpression.getCondition() as KtPrefixExpression
        ifCondition.getBaseExpression()!!.replace(psiFactory.createExpression(conditionText))

        val thrownExpression = ((ifExpression.getThen() as KtBlockExpression).getStatements().single() as KtThrowExpression).getThrownExpression()
        val assertionErrorCall = if (thrownExpression is KtCallExpression)
            thrownExpression
        else
            (thrownExpression as KtDotQualifiedExpression).getSelectorExpression() as KtCallExpression

        val message = psiFactory.createExpression(
                if (messageIsFunction && messageExpr is KtCallableReferenceExpression) {
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

    private fun extractMessageSingleExpression(functionLiteral: KtFunctionLiteralExpression, bindingContext: BindingContext): KtExpression? {
        return functionLiteral.bodyExpression?.statements?.singleOrNull()?.let { singleStatement ->
            singleStatement.check { it.isUsedAsExpression(bindingContext) }
        }
    }

    private fun messageIsFunction(callExpr: KtCallExpression, bindingContext: BindingContext): Boolean {
        val resolvedCall = callExpr.getResolvedCall(bindingContext) ?: return false
        val valParameters = resolvedCall.getResultingDescriptor().getValueParameters()
        return valParameters.size() > 1 && !KotlinBuiltIns.isAny(valParameters[1].type)
    }

    private fun simplifyConditionIfPossible(ifExpression: KtIfExpression) {
        val condition = ifExpression.getCondition() as KtPrefixExpression
        val simplifier = SimplifyNegatedBinaryExpressionIntention()
        if (simplifier.isApplicableTo(condition)) {
            simplifier.applyTo(condition)
        }
    }

    private fun replaceWithIfThenThrowExpression(original: KtCallExpression): KtIfExpression {
        val replacement = KtPsiFactory(original).createExpression("if (!true) { throw java.lang.AssertionError(\"\") }") as KtIfExpression
        val parent = original.getParent()
        return if (parent is KtDotQualifiedExpression)
            parent.replaced(replacement)
        else
            original.replaced(replacement)
    }
}
