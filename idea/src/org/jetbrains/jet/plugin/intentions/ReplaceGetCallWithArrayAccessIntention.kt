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
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetValueArgument
import org.jetbrains.jet.lang.psi.JetValueArgumentList
import org.jetbrains.jet.lang.psi.JetNamedArgumentImpl
import org.jetbrains.jet.lang.psi.JetValueArgumentName
import org.jetbrains.jet.plugin.references.JetSimpleNameReference
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lang.psi.JetPostfixExpression
import org.jetbrains.jet.lang.psi.JetTypeArgumentList

public class ReplaceGetCallWithArrayAccessIntention : JetSelfTargetingIntention<JetExpression>("replace.get.call.with.array.access", javaClass()) {
    override fun isApplicableTo(element: JetExpression): Boolean {
        fun methodCallCheck(expression: JetDotQualifiedExpression): Boolean {
            val selector = expression.getSelectorExpression()
            val receiver = expression.getReceiverExpression()

            if (selector is JetCallExpression && !(receiver is JetPostfixExpression)) {
                val callee = selector.getCalleeExpression()
                val arguments = selector.getValueArgumentList()
                val typeArguments = selector.getTypeArgumentList()

                return arguments != null
                    && typeArguments == null
                    && !arguments.getArguments().any { (arg): Boolean -> arg.getArgumentName() != null }
                    && callee != null
                    && callee.textMatches("get")
            } else {
                return false
            }
        }

        fun infixOperatorCheck(expression: JetBinaryExpression): Boolean {
            return expression.getOperationReference().textMatches("get") && expression.getLeft() != null && expression.getRight() != null
        }

        val parent = element.getParent()
        when (parent) {
            is JetDotQualifiedExpression -> return methodCallCheck(parent)
            is JetBinaryExpression -> return infixOperatorCheck(parent)
            else -> return false
        }
    }

    override fun applyTo(element: JetExpression, editor: Editor) {
        fun applyToMethodCall(expression: JetDotQualifiedExpression) {
            val selector = expression.getSelectorExpression() as JetCallExpression
            val arguments = selector.getValueArgumentList()
            val functionLiteralArguments = selector.getFunctionLiteralArguments()
            val argumentsText = arguments!!.getText()
            val project = element.getProject()
            val receiver = expression.getReceiverExpression()
            val arrayArgumentsTextStringBuilder = StringBuilder("[${argumentsText!!.substring(1, argumentsText.length - 1)}")

            for (functionLiteral in functionLiteralArguments) {
                arrayArgumentsTextStringBuilder.append(", ${functionLiteral.getText()}")
            }
            arrayArgumentsTextStringBuilder.append("]")

            val replacement = JetPsiFactory.createExpression(project, receiver.getText() + arrayArgumentsTextStringBuilder.toString())
            expression.replace(replacement)
        }

        fun applyToInfixCall(expression: JetBinaryExpression) {
            val arrayArgumentsText = "[${expression.getRight()!!.getText()}]"
            val project = expression.getProject()
            val receiver = expression.getLeft()
            val replacement = JetPsiFactory.createExpression(project, receiver!!.getText() + arrayArgumentsText)
            expression.replace(replacement)
        }

        val parent = element.getParent()
        when (parent) {
            is JetDotQualifiedExpression -> applyToMethodCall(parent)
            is JetBinaryExpression -> applyToInfixCall(parent)
        }
    }
}