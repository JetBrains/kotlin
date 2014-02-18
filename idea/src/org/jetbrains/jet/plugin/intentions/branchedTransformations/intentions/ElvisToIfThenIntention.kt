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

import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.plugin.intentions.JetSelfTargetingIntention
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.plugin.refactoring.introduceVariable.JetIntroduceVariableHandler
import org.jetbrains.jet.lang.psi.JetIfExpression
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.psi.JetPsiUtil
import com.intellij.openapi.project.Project

public class ElvisToIfThenIntention : JetSelfTargetingIntention<JetBinaryExpression>("elvis.to.conditional", javaClass()) {
    override fun isApplicableTo(element: JetBinaryExpression): Boolean =
        element.getOperationToken() == JetTokens.ELVIS

    override fun applyTo(element: JetBinaryExpression, editor: Editor) {
        val lhs = JetPsiUtil.deparenthesize(element.getLeft())
        when (lhs) {
            is JetSimpleNameExpression -> convertToIfStatement(element)
            is JetExpression -> {
                val ifStatement = convertToIfStatement(element)
                introduceValueForLhs(ifStatement, element.getProject(), editor)
            }
        }
    }

    fun introduceValueForLhs(ifStatement: JetIfExpression, project: Project, editor: Editor) {
        val occurrenceInConditional = ifStatement.getCondition()?.getFirstChild() as JetExpression
        val occurrenceInIfClause = ifStatement.getThen() as JetExpression
        JetIntroduceVariableHandler().invoke(project, editor, occurrenceInConditional, listOf(occurrenceInIfClause))
    }

    fun convertToIfStatement(element: JetBinaryExpression): JetIfExpression {
        val lhs = checkNotNull(element.getLeft(), "Element must exist on left hand side of elvis expresssion")
        val rhs = checkNotNull(element.getRight(), "Element must exist on right had side of elvis expression")
        val conditionalString =
                if (lhs is JetBinaryExpression)
                    "if (${lhs.getText()} != null) (${lhs.getText()}) else ${rhs.getText()}"
                else
                    "if (${lhs.getText()} != null) ${lhs.getText()} else ${rhs.getText()}"

        val resultingExpression = JetPsiFactory.createExpression(element.getProject(), conditionalString)
        return element.replace(resultingExpression) as JetIfExpression
    }
}
