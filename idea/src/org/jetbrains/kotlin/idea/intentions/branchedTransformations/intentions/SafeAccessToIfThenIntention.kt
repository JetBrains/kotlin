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
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingOffsetIndependentIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.convertToIfNotNullExpression
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.introduceValueForCondition
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isStableVariable
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsStatement

public class SafeAccessToIfThenIntention : JetSelfTargetingOffsetIndependentIntention<JetSafeQualifiedExpression>("safe.access.to.if.then", javaClass()) {
    override fun isApplicableTo(element: JetSafeQualifiedExpression): Boolean = true

    override fun applyTo(element: JetSafeQualifiedExpression, editor: Editor) {
        val receiver = JetPsiUtil.deparenthesize(element.getReceiverExpression())!!
        val selector = JetPsiUtil.deparenthesize(element.getSelectorExpression())

        val receiverIsStable = receiver.isStableVariable()

        val receiverTemplate = if (receiver is JetBinaryExpression) "(%s)" else "%s"
        val receiverAsString = receiverTemplate.format(receiver.getText())
        val psiFactory = JetPsiFactory(element)
        val dotQualifiedExpression = psiFactory.createExpression("${receiverAsString}.${selector!!.getText()}")

        val elseClause = if (element.isUsedAsStatement(element.analyze())) null else psiFactory.createExpression("null")
        val ifExpression = element.convertToIfNotNullExpression(receiver, dotQualifiedExpression, elseClause)

        if (!receiverIsStable) {
            val valueToExtract = (ifExpression.getThen() as JetDotQualifiedExpression).getReceiverExpression()
            ifExpression.introduceValueForCondition(valueToExtract, editor)
        }
    }
}

