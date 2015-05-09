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
import java.util.ArrayList

public class FoldWhenToAssignmentIntention : JetSelfTargetingOffsetIndependentIntention<JetWhenExpression>(javaClass(), "Replace 'when' expression with assignment") {
    override fun isApplicableTo(element: JetWhenExpression): Boolean {
        if (!JetPsiUtil.checkWhenExpressionHasSingleElse(element)) return false

        val entries = element.getEntries()

        if (entries.isEmpty()) return false

        val assignments = ArrayList<JetBinaryExpression>()
        for (entry in entries) {
            val assignment = BranchedFoldingUtils.getFoldableBranchedAssignment(entry.getExpression()) ?: return false
            assignments.add(assignment)
        }

        assert(!assignments.isEmpty())

        val firstAssignment = assignments.get(0)
        for (assignment in assignments) {
            if (!BranchedFoldingUtils.checkAssignmentsMatch(assignment, firstAssignment)) return false
        }

        return true
    }

    override fun applyTo(element: JetWhenExpression, editor: Editor) {
        assert(!element.getEntries().isEmpty())

        val firstAssignment = BranchedFoldingUtils.getFoldableBranchedAssignment(element.getEntries().get(0).getExpression()!!)!!

        val op = firstAssignment.getOperationReference().getText()
        val lhs = firstAssignment.getLeft() as JetSimpleNameExpression

        val assignment = JetPsiFactory(element).createExpressionByPattern("$0 $1 $2", lhs, op, element)
        val newWhenExpression = (assignment as JetBinaryExpression).getRight() as JetWhenExpression

        for (entry in newWhenExpression.getEntries()) {
            val currAssignment = BranchedFoldingUtils.getFoldableBranchedAssignment(entry.getExpression()!!)!!
            val currRhs = currAssignment.getRight()!!
            currAssignment.replace(currRhs)
        }

        element.replace(assignment)
    }
}