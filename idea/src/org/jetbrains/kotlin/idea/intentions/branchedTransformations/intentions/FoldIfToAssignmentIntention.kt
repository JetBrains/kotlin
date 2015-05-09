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
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingOffsetIndependentIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.BranchedFoldingUtils
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.psi.JetIfExpression
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.createExpressionByPattern

public class FoldIfToAssignmentIntention : JetSelfTargetingOffsetIndependentIntention<JetIfExpression>(javaClass(), "Replace 'if' expression with assignment") {
    override fun isApplicableTo(element: JetIfExpression): Boolean {
        val thenBranch = element.getThen()
        val elseBranch = element.getElse()

        val thenAssignment = BranchedFoldingUtils.getFoldableBranchedAssignment(thenBranch)
        val elseAssignment = BranchedFoldingUtils.getFoldableBranchedAssignment(elseBranch)

        if (thenAssignment == null || elseAssignment == null) return false

        return BranchedFoldingUtils.checkAssignmentsMatch(thenAssignment, elseAssignment)
    }

    override fun applyTo(element: JetIfExpression, editor: Editor) {
        var thenAssignment = BranchedFoldingUtils.getFoldableBranchedAssignment(element.getThen()!!)!!

        val op = thenAssignment.getOperationReference().getText()
        val lhs = thenAssignment.getLeft() as JetSimpleNameExpression

        val assignment = psi.JetPsiFactory(element).createExpressionByPattern("$0 $1 $2", lhs, op, element)
        val newIfExpression = (assignment as JetBinaryExpression).getRight() as JetIfExpression

        thenAssignment = BranchedFoldingUtils.getFoldableBranchedAssignment(newIfExpression.getThen()!!)!!
        val elseAssignment = BranchedFoldingUtils.getFoldableBranchedAssignment(newIfExpression.getElse()!!)!!

        val thenRhs = thenAssignment.getRight()!!
        val elseRhs = elseAssignment.getRight()!!

        thenAssignment.replace(thenRhs)
        elseAssignment.replace(elseRhs)

        element.replace(assignment)
    }
}