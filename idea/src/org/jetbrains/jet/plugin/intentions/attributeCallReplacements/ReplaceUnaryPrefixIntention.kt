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

public open class ReplaceUnaryPrefixIntention : AttributeCallReplacementIntention("replace.unary.operator.with.prefix") {

    private fun lookup(name: String?) : String? {
        return when (name) {
            "plus" -> "+"
            "minus" -> "-"
            "not" -> "!"
            else -> null
        }
    }

    override fun formatArgumentsFor(call: CallDescription): Array<Any?> {
        return array(lookup(call.functionName))
    }

    override fun isApplicableToCall(call: CallDescription): Boolean {
        return (
            lookup(call.functionName) != null &&
            !call.hasTypeArguments &&
            call.argumentCount == 0 &&
            call.callElement.getValueArgumentList() != null // Has argument expression
        )
    }

    override fun replaceCall(call: CallDescription, editor: Editor) {
        call.element.replace(JetPsiFactory(call.element).createExpression(
                lookup(call.functionName)!! + call.element.getReceiverExpression().getText()
        ))
    }
}
