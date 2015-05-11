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

package org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.convertToIfNotNullExpression
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.introduceValueForCondition
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isStableVariable
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsStatement

public class SafeAccessToIfThenIntention : JetSelfTargetingRangeIntention<JetSafeQualifiedExpression>(javaClass(), "Replace safe access expression with 'if' expression") {
    override fun applicabilityRange(element: JetSafeQualifiedExpression): TextRange? {
        if (element.getSelectorExpression() == null) return null
        return element.getOperationTokenNode().getTextRange()
    }

    override fun applyTo(element: JetSafeQualifiedExpression, editor: Editor) {
        val receiver = JetPsiUtil.safeDeparenthesize(element.getReceiverExpression())
        val selector = element.getSelectorExpression()!!

        val receiverIsStable = receiver.isStableVariable()

        val psiFactory = JetPsiFactory(element)
        val dotQualified = psiFactory.createExpressionByPattern("$0.$1", receiver, selector)

        val elseClause = if (element.isUsedAsStatement(element.analyze())) null else psiFactory.createExpression("null")
        val ifExpression = element.convertToIfNotNullExpression(receiver, dotQualified, elseClause)

        if (!receiverIsStable) {
            val valueToExtract = (ifExpression.getThen() as JetDotQualifiedExpression).getReceiverExpression()
            ifExpression.introduceValueForCondition(valueToExtract, editor)
        }
    }
}

