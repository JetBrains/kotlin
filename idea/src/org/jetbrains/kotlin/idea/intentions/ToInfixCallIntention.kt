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

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*

public class ToInfixCallIntention : JetSelfTargetingIntention<JetCallExpression>(javaClass(), "Replace with infix function call") {
    override fun isApplicableTo(element: JetCallExpression, caretOffset: Int): Boolean {
        val calleeExpr = element.getCalleeExpression() as? JetSimpleNameExpression ?: return false
        if (!calleeExpr.getTextRange().containsOffset(caretOffset)) return false

        val dotQualified = element.getParent() as? JetDotQualifiedExpression ?: return false

        if (element.getTypeArgumentList() != null) return false

        val argument = element.getValueArguments().singleOrNull() ?: return false
        if (argument.isNamed()) return false
        if (argument.getArgumentExpression() == null) return false

        // check that receiver has type to filter out calls with package/java class qualifier
        val receiver = dotQualified.getReceiverExpression()
        if (element.analyze().getType(receiver) == null) return false

        return true
    }

    override fun applyTo(element: JetCallExpression, editor: Editor) {
        val dotQualified = element.getParent() as JetDotQualifiedExpression
        val receiver = dotQualified.getReceiverExpression()
        val argument = element.getValueArguments().single().getArgumentExpression()!!
        val name = element.getCalleeExpression()!!.getText()

        val newCall = JetPsiFactory(element).createExpressionByPattern("$0 $name $1", receiver, argument)
        dotQualified.replace(newCall)
    }
}
