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

public class ReplaceGetCallWithArrayAccessIntention : JetSelfTargetingIntention<JetDotQualifiedExpression>("replace.get.call.with.array.access", javaClass()) {
    override fun isApplicableTo(element: JetDotQualifiedExpression): Boolean {
        val selector = element.getSelectorExpression()

        fun checkSelector(selector: JetCallExpression): Boolean {
            try {
                val callee: JetExpression? = selector.getCalleeExpression()
                val arguments: JetValueArgumentList? = selector.getValueArgumentList()

                if (arguments == null) {
                    return false
                } else {
                    for (arg: JetValueArgument in arguments.getArguments()) {
                        if (arg.getArgumentName() != null) {
                            return false
                        }
                    }
                }

                return callee!!.textMatches("get")
            } catch (e: Exception) {
                return false
            }
        }

        when(selector) {
            is JetCallExpression -> return checkSelector(selector)
            else -> return false
        }
    }

    override fun applyTo(element: JetDotQualifiedExpression, editor: Editor) {
        val selector: JetCallExpression? = element.getSelectorExpression() as? JetCallExpression
        val arguments = selector?.getValueArgumentList()
        assert(arguments != null)
        val argumentsText = arguments!!.getText()
        assert(argumentsText != null)
        val receiver: JetExpression = element.getReceiverExpression()
        var arrayArgumentsText = ""
        val project = element.getProject()

        for (i in 0..argumentsText!!.length() - 1) {
            when (i) {
                0 -> arrayArgumentsText += "["
                argumentsText.length() - 1 -> arrayArgumentsText += "]"
                else -> arrayArgumentsText += argumentsText[i]
            }
        }

        val replacement = JetPsiFactory.createExpression(project, receiver.getText() + arrayArgumentsText)
        element.replace(replacement)
    }
}
