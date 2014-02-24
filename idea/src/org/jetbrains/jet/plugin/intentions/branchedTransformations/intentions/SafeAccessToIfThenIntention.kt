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
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.plugin.refactoring.introduceVariable.JetIntroduceVariableHandler
import org.jetbrains.jet.lang.psi.JetIfExpression
import org.jetbrains.jet.lang.psi.JetPsiUtil
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import com.intellij.openapi.project.Project

public class SafeAccessToIfThenIntention: JetSelfTargetingIntention<JetSafeQualifiedExpression>("safe.access.to.if.then", javaClass()) {
    override fun isApplicableTo(element: JetSafeQualifiedExpression): Boolean = true

    override fun applyTo(element: JetSafeQualifiedExpression, editor: Editor) {
        val receiver = JetPsiUtil.deparenthesize(element.getReceiverExpression())
        when (receiver) {
            is JetSimpleNameExpression -> convertToIfStatement(element, receiver)
            is JetExpression -> {
                val ifStatement = convertToIfStatement(element, receiver)
                introduceValueForReceiver(ifStatement, element.getProject(), editor)
            }
        }
    }

    fun introduceValueForReceiver(ifStatement: JetIfExpression, project: Project, editor: Editor) {
        val occurrenceInConditional = ifStatement.getCondition()?.getFirstChild() as JetExpression
        val occurrenceInIfClause = ifStatement.getThen()?.getFirstChild()  as JetExpression
        JetIntroduceVariableHandler().invoke(project, editor, occurrenceInConditional, listOf(occurrenceInIfClause))
    }

    fun convertToIfStatement(element: JetSafeQualifiedExpression, receiver: JetExpression): JetIfExpression {
        val selector = element.getSelectorExpression()
        val conditionalString =
            if (receiver is JetBinaryExpression)
                "if (${receiver.getText()} != null) (${receiver.getText()}).${selector?.getText()} else null"
            else
                "if (${receiver.getText()} != null) ${receiver.getText()}.${selector?.getText()} else null"

        val resultingExpression = JetPsiFactory.createExpression(element.getProject(), conditionalString)
        return element.replace(resultingExpression) as JetIfExpression
    }
}
