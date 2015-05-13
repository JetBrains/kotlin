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

package org.jetbrains.kotlin.idea.intentions.conventionNameCalls

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.intentions.calleeName
import org.jetbrains.kotlin.psi.JetDotQualifiedExpression
import org.jetbrains.kotlin.types.expressions.OperatorConventions

public class ReplaceInvokeIntention : JetSelfTargetingRangeIntention<JetDotQualifiedExpression>(javaClass(), "Replace 'invoke' with direct call") {
    override fun applicabilityRange(element: JetDotQualifiedExpression): TextRange? {
        if (element.calleeName != OperatorConventions.INVOKE.asString()) return null
        return element.callExpression!!.getCalleeExpression()!!.getTextRange()
    }

    override fun applyTo(element: JetDotQualifiedExpression, editor: Editor) {
        val receiver = element.getReceiverExpression()
        val callExpression = element.callExpression!!
        callExpression.getCalleeExpression()!!.replace(receiver)
        element.replace(callExpression)
    }
}
