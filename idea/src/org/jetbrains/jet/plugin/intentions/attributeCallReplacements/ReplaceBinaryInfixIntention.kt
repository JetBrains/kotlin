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

package org.jetbrains.jet.plugin.intentions.attributeCallReplacements

import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetPsiFactory

public open class ReplaceBinaryInfixIntention : AttributeCallReplacementIntention("replace.binary.operator.with.infix") {

    private fun lookup(name: String?): String? {
        return when (name) {
            "plus" -> "+"
            "minus" -> "-"
            "div" -> "/"
            "times" -> "*"
            "mod" -> "%"
            "rangeTo" -> ".."
            else -> null
        }
    }

    override fun formatArgumentsFor(call: CallDescription): Array<Any?> {
        return array(lookup(call.functionName))
    }

    override fun isApplicableToCall(call: CallDescription): Boolean {
        return (
            lookup(call.functionName) != null &&
            call.argumentCount == 1 &&
            !call.hasTypeArguments &&
            !call.hasEmptyArguments
        )
    }

    override fun replaceCall(call: CallDescription, editor: Editor) {
        val argument = (handleErrors(editor, call.getPositionalArguments()) ?: return)[0].getArgumentExpression()

        call.element.replace(
                JetPsiFactory(call.element).createBinaryExpression(
                        call.element.getReceiverExpression(),
                        lookup(call.functionName)!!,  // Lookup must succeed
                        argument
                )
        )
    }
}
