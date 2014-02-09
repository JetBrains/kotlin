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

package org.jetbrains.jet.plugin.intentions.branchedTransformations.intentions

import org.jetbrains.jet.lang.psi.JetSafeQualifiedExpression
import org.jetbrains.jet.plugin.intentions.JetSelfTargetingIntention
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.psi.JetParenthesizedExpression
import org.jetbrains.jet.lang.psi.JetExpression
import com.intellij.psi.PsiElement
import org.jetbrains.jet.plugin.refactoring.introduceVariable.JetIntroduceVariableHandler
import org.jetbrains.jet.plugin.refactoring.introduceVariable.JetInplaceVariableIntroducer
import com.intellij.ide.DataManager
import org.jetbrains.jet.lang.psi.JetIfExpression

public class ElvisToConditionalIntention: JetSelfTargetingIntention<JetSafeQualifiedExpression>("elvis.to.conditional", javaClass()) {
    override fun isApplicableTo(element: JetSafeQualifiedExpression): Boolean = true

    override fun applyTo(element: JetSafeQualifiedExpression, editor: Editor) {
        val receiver = element.getReceiverExpression()
        when (receiver) {
            is JetSimpleNameExpression -> applyToSimpleReceiver(element, receiver)
            is JetParenthesizedExpression -> {
                val receiverExpression = receiver.getExpression()
                if (receiverExpression is JetSimpleNameExpression) {
                    applyToSimpleReceiver(element, receiverExpression)
                } else if (receiverExpression != null){
                    applyToNonSimpleReceiver(element, receiver, editor)
                }
            }
            else -> applyToNonSimpleReceiver(element, receiver, editor)
        }
    }

    fun applyToNonSimpleReceiver(element: JetSafeQualifiedExpression, receiver: JetExpression, editor: Editor) {
        val conditionalExp = applyToSimpleReceiver(element, receiver)
        val occurrenceInConditional = conditionalExp.getCondition()?.getFirstChild() as JetExpression
        val occurrenceInIfClause = conditionalExp.getThen()?.getFirstChild()  as JetExpression
        JetIntroduceVariableHandler().invoke(element.getProject(), editor, occurrenceInConditional, listOf(occurrenceInIfClause))
    }

    fun applyToSimpleReceiver(element: JetSafeQualifiedExpression, receiver: JetExpression): JetIfExpression {
        val selector = element.getSelectorExpression()
        val conditionalString = "if (${receiver.getText()} != null) ${receiver.getText()}.${selector?.getText()} else null"
        val resultingExpression = JetPsiFactory.createExpression(element.getProject(), conditionalString)
        return element.replace(resultingExpression) as JetIfExpression
    }
}
