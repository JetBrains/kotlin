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

import org.jetbrains.jet.lang.psi.JetCallExpression
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetQualifiedExpression
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.plugin.intentions.JetSelfTargetingIntention
import com.intellij.openapi.ui.popup.JBPopupFactory

public abstract class AttributeCallReplacementIntention(name: String) : JetSelfTargetingIntention<JetDotQualifiedExpression>(name, javaClass()) {

    class ReplacementException(message: String) : Exception(message)

    protected abstract fun isApplicableToCall(call: JetCallExpression): Boolean

    protected abstract fun replaceCall(element: JetQualifiedExpression, call: JetCallExpression, receiver: JetExpression): Unit

    final override fun isApplicableTo(element: JetDotQualifiedExpression): Boolean {
        val call = element.getSelectorExpression()
        return call is JetCallExpression && isApplicableToCall(call)
    }

    final override fun applyTo(element: JetDotQualifiedExpression, editor: Editor) {
        try {
            replaceCall(element, element.getSelectorExpression() as JetCallExpression, element.getReceiverExpression())
        } catch (e : ReplacementException) {
            JBPopupFactory.getInstance()!!.createMessage(e.getMessage() ?: "Intention Failed").showInBestPositionFor(editor)
        }
    }

    // Helper Functions

    /**
     * Get the name of a call expression if defined.
     */
    protected fun JetCallExpression.getFunctionName(): String? {
        return this.getCalleeExpression()?.getText()
    }

    /**
     * Returns true iff the call expression has named arguments.
     */
    protected fun JetCallExpression.hasNamedArguments(): Boolean {
        return getValueArguments().any { arg -> arg?.getArgumentName() != null }
    }

    /**
     * Returns the total number of arguments.
     */
    protected fun JetCallExpression.getArgumentCount(): Int {
        return getValueArguments().size + this.getFunctionLiteralArguments().size
    }

    /**
     * Replaces this with the given string
     */
    protected fun JetExpression.replace(text: String) {
        replace(JetPsiFactory.createExpression(getProject(), text))
    }
}
