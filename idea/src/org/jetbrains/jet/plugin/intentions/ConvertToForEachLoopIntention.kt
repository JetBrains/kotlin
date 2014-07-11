/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.psi.JetFunctionLiteralExpression
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.plugin.caches.resolve.getLazyResolveSession
import org.jetbrains.jet.lang.psi.JetParenthesizedExpression
import org.jetbrains.jet.lang.resolve.bindingContextUtil.getResolvedCall

public class ConvertToForEachLoopIntention : JetSelfTargetingIntention<JetExpression>("convert.to.for.each.loop.intention", javaClass()) {
    private fun getFunctionLiteralArgument(element: JetExpression): JetFunctionLiteralExpression? {
        val argument = when (element) {
            is JetDotQualifiedExpression -> {
                val selector = element.getSelectorExpression()

                when (selector) {
                    is JetCallExpression -> {
                        when {
                            selector.getValueArguments().size() > 0 -> selector.getValueArguments()[0]!!.getArgumentExpression()
                            selector.getFunctionLiteralArguments().size() > 0 -> selector.getFunctionLiteralArguments()[0]
                            else -> null
                        }
                    }
                    else -> null
                }
            }
            is JetBinaryExpression -> element.getRight()
            else -> null
        }

        return when (argument) {
            is JetFunctionLiteralExpression -> argument
            else -> null
        }
    }

    override fun isApplicableTo(element: JetExpression): Boolean {
        fun isWellFormedFunctionLiteral(element: JetFunctionLiteralExpression): Boolean {
            return element.getValueParameters().size() <= 1 && element.getBodyExpression() != null
        }

        fun checkTotalNumberOfArguments(element: JetExpression): Boolean {
            return when (element) {
                is JetDotQualifiedExpression -> {
                    val selector = element.getSelectorExpression()

                    when (selector) {
                        is JetCallExpression -> (selector.getValueArguments().size() + selector.getFunctionLiteralArguments().size()) == 1
                        else -> false
                    }
                }
                is JetBinaryExpression -> true
                else -> false
            }
        }

        val functionLiteral = getFunctionLiteralArgument(element)

        if (functionLiteral != null &&
            isWellFormedFunctionLiteral(functionLiteral) &&
            checkTotalNumberOfArguments(element)) {

            val context = element.getContainingJetFile().getLazyResolveSession().resolveToElement(element)
            val resolvedCall = element.getResolvedCall(context)
            val functionFqName = if (resolvedCall != null) DescriptorUtils.getFqName(resolvedCall.getResultingDescriptor()).toString() else null

            return "kotlin.forEach".equals(functionFqName);
        }

        return false;
    }

    override fun applyTo(element: JetExpression, editor: Editor) {
        fun buildLoopRangeText(receiver: JetExpression): String? {
            return when (receiver) {
                is JetParenthesizedExpression -> receiver.getExpression()?.getText()
                else -> receiver.getText()
            }
        }

        fun generateLoopText(receiver: JetExpression, functionLiteral: JetFunctionLiteralExpression): String {
            return when {
                functionLiteral.getValueParameters().size() == 0 -> "for (it in ${buildLoopRangeText(receiver)}) { ${functionLiteral.getBodyExpression()!!.getText()} }"
                else -> "for (${functionLiteral.getValueParameters()[0].getText()} in ${buildLoopRangeText(receiver)}) { ${functionLiteral.getBodyExpression()!!.getText()} }"
            }
        }

        val receiver = when (element) {
            is JetDotQualifiedExpression -> element.getReceiverExpression()
            is JetBinaryExpression -> element.getLeft()
            else -> throw IllegalArgumentException("The expression ${element.getText()} cannot be converted to a for each loop.")
        }!!

        val functionLiteral = getFunctionLiteralArgument(element)!!

        element.replace(JetPsiFactory(element).createExpression(generateLoopText(receiver, functionLiteral)))
    }
}