/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.intentions.calleeName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.util.OperatorNameConventions

class ReplaceInvokeIntention : SelfTargetingRangeIntention<KtDotQualifiedExpression>(KtDotQualifiedExpression::class.java, "Replace 'invoke' with direct call"), HighPriorityAction {
    override fun applicabilityRange(element: KtDotQualifiedExpression): TextRange? {
        if (element.calleeName != OperatorNameConventions.INVOKE.asString() || element.callExpression?.typeArgumentList != null) return null
        return element.callExpression!!.calleeExpression!!.textRange
    }

    override fun applyTo(element: KtDotQualifiedExpression, editor: Editor?) {
        val receiver = element.receiverExpression
        val callExpression = element.callExpression!!.copy() as KtCallExpression
        callExpression.calleeExpression!!.replace(receiver)
        element.replace(callExpression)
    }
}
