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
import org.jetbrains.kotlin.psi.*

public class FoldIfToAssignmentIntention : JetSelfTargetingOffsetIndependentIntention<JetIfExpression>(javaClass(), "Replace 'if' expression with assignment") {
    override fun isApplicableTo(element: JetIfExpression): Boolean {
        val thenAssignment = BranchedFoldingUtils.getFoldableBranchedAssignment(element.getThen()) ?: return false
        val elseAssignment = BranchedFoldingUtils.getFoldableBranchedAssignment(element.getElse()) ?: return false
        return BranchedFoldingUtils.checkAssignmentsMatch(thenAssignment, elseAssignment)
    }

    override fun applyTo(element: JetIfExpression, editor: Editor) {
        var thenAssignment = BranchedFoldingUtils.getFoldableBranchedAssignment(element.getThen()!!)!!
        val elseAssignment = BranchedFoldingUtils.getFoldableBranchedAssignment(element.getElse()!!)!!

        val op = thenAssignment.getOperationReference().getText()
        val leftText = thenAssignment.getLeft()!!.getText()

        thenAssignment.replace(thenAssignment.getRight()!!)
        elseAssignment.replace(elseAssignment.getRight()!!)

        element.replace(JetPsiFactory(element).createExpressionByPattern("$0 $1 $2", leftText, op, element))
    }
}