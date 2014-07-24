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

public open class ReplaceInvokeIntention : AttributeCallReplacementIntention("replace.invoke.with.call") {

    override fun isApplicableToCall(call: CallDescription): Boolean {
        return call.functionName == "invoke"
    }

    override fun replaceCall(call: CallDescription, editor: Editor) {
        call.element.replace(JetPsiFactory(call.element).createExpression(
                call.element.getReceiverExpression().getText() +
                (call.callElement.getTypeArgumentList()?.getText() ?: "") +
                (call.callElement.getValueArgumentList()?.getText() ?: "") +
                (call.callElement.getFunctionLiteralArguments().fold("") { a, b -> a + " " + b.getText() })
        ))
    }
}
