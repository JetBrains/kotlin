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

package org.jetbrains.kotlin.idea.intentions.attributeCallReplacements

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingOffsetIndependentIntention
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.intentions.functionName
import org.jetbrains.kotlin.psi.JetDotQualifiedExpression
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.buildExpression
import org.jetbrains.kotlin.psi.createExpressionByPattern

public class ReplaceGetIntention : JetSelfTargetingOffsetIndependentIntention<JetDotQualifiedExpression>(javaClass(), "Replace 'get' call with index operator") {
    override fun isApplicableTo(element: JetDotQualifiedExpression): Boolean {
        if (element.functionName != "get") return false
        val call = element.callExpression ?: return false
        if (call.getTypeArgumentList() != null) return false
        val arguments = call.getValueArguments()
        if (arguments.isEmpty()) return false
        if (arguments.any { it.isNamed() }) return false
        return true
    }

    override fun applyTo(element: JetDotQualifiedExpression, editor: Editor) {
        val expression = JetPsiFactory(element).buildExpression {
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
