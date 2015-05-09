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

package org.jetbrains.kotlin.idea.intentions.branchedTransformations

import com.google.common.base.Predicate
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.*

import java.util.ArrayList

public object BranchedFoldingUtils {
    private val CHECK_ASSIGNMENT = object : Predicate<JetElement> {
        override fun apply(input: JetElement): Boolean {
            if (!JetPsiUtil.isAssignment(input)) return false

            val assignment = input as JetBinaryExpression

            val left = assignment.getLeft() as? JetSimpleNameExpression ?: return false
            if (assignment.getRight() == null) return false

            val parent = assignment.getParent()
            if (parent is JetBlockExpression) {
                return !JetPsiUtil.checkVariableDeclarationInBlock(parent, left.getText())
            }

            return true
        }
    }

    private fun getFoldableBranchedAssignment(branch: JetExpression?): JetBinaryExpression? {
        return JetPsiUtil.getOutermostLastBlockElement(branch, CHECK_ASSIGNMENT) as JetBinaryExpression?
    }

    private fun getFoldableBranchedReturn(branch: JetExpression?): JetReturnExpression? {
        return JetPsiUtil.getOutermostLastBlockElement(branch) {
            (it as? JetReturnExpression)?.getReturnedExpression() != null
        } as JetReturnExpression?
    }

    private fun checkAssignmentsMatch(a1: JetBinaryExpression, a2: JetBinaryExpression): Boolean {
        return a1.getLeft()?.getText() == a2.getLeft()?.getText() && a1.getOperationToken() == a2.getOperationToken()
    }

    private fun checkFoldableIfExpressionWithAssignments(ifExpression: JetIfExpression): Boolean {
        val thenBranch = ifExpression.getThen()
        val elseBranch = ifExpression.getElse()

        val thenAssignment = getFoldableBranchedAssignment(thenBranch)
        val elseAssignment = getFoldableBranchedAssignment(elseBranch)

        if (thenAssignment == null || elseAssignment == null) return false

        return checkAssignmentsMatch(thenAssignment, elseAssignment)
    }

    private fun checkFoldableWhenExpressionWithAssignments(whenExpression: JetWhenExpression): Boolean {
        if (!JetPsiUtil.checkWhenExpressionHasSingleElse(whenExpression)) return false

        val entries = whenExpression.getEntries()

        if (entries.isEmpty()) return false

        val assignments = ArrayList<JetBinaryExpression>()
        for (entry in entries) {
            val assignment = getFoldableBranchedAssignment(entry.getExpression()) ?: return false
            assignments.add(assignment)
        }

        assert(!assignments.isEmpty())

        val firstAssignment = assignments.get(0)
        for (assignment in assignments) {
            if (!checkAssignmentsMatch(assignment, firstAssignment)) return false
        }

        return true
    }

    private fun checkFoldableIfExpressionWithReturns(ifExpression: JetIfExpression): Boolean {
        return getFoldableBranchedReturn(ifExpression.getThen()) != null && getFoldableBranchedReturn(ifExpression.getElse()) != null
    }

    private fun checkFoldableWhenExpressionWithReturns(whenExpression: JetWhenExpression): Boolean {
        if (!JetPsiUtil.checkWhenExpressionHasSingleElse(whenExpression)) return false

        val entries = whenExpression.getEntries()

        if (entries.isEmpty()) return false

        for (entry in entries) {
            if (getFoldableBranchedReturn(entry.getExpression()) == null) return false
        }

        return true
    }

    private fun checkFoldableIfExpressionWithAsymmetricReturns(ifExpression: JetIfExpression): Boolean {
        if (getFoldableBranchedReturn(ifExpression.getThen()) == null || ifExpression.getElse() != null) {
            return false
        }

        val nextElement = JetPsiUtil.skipTrailingWhitespacesAndComments(ifExpression)
        return (nextElement is JetExpression) && getFoldableBranchedReturn(nextElement) != null
    }

    public fun getFoldableExpressionKind(root: JetExpression?): FoldableKind? {
        if (root is JetIfExpression) {

            if (checkFoldableIfExpressionWithAssignments(root)) return FoldableKind.IF_TO_ASSIGNMENT
            if (checkFoldableIfExpressionWithReturns(root)) return FoldableKind.IF_TO_RETURN
            if (checkFoldableIfExpressionWithAsymmetricReturns(root)) return FoldableKind.IF_TO_RETURN_ASYMMETRICALLY
        }
        else if (root is JetWhenExpression) {

            if (checkFoldableWhenExpressionWithAssignments(root)) return FoldableKind.WHEN_TO_ASSIGNMENT
            if (checkFoldableWhenExpressionWithReturns(root)) return FoldableKind.WHEN_TO_RETURN
        }

        return null
    }

    public fun foldIfExpressionWithAssignments(ifExpression: JetIfExpression) {
        var thenAssignment = getFoldableBranchedAssignment(ifExpression.getThen()!!)!!

        val op = thenAssignment.getOperationReference().getText()
        val lhs = thenAssignment.getLeft() as JetSimpleNameExpression

        val assignment = JetPsiFactory(ifExpression).createExpressionByPattern("$0 $1 $2", lhs, op, ifExpression)
        val newIfExpression = (assignment as JetBinaryExpression).getRight() as JetIfExpression

        thenAssignment = getFoldableBranchedAssignment(newIfExpression.getThen()!!)!!
        val elseAssignment = getFoldableBranchedAssignment(newIfExpression.getElse()!!)!!

        val thenRhs = thenAssignment.getRight()!!
        val elseRhs = elseAssignment.getRight()!!

        thenAssignment.replace(thenRhs)
        elseAssignment.replace(elseRhs)

        ifExpression.replace(assignment)
    }

    public fun foldIfExpressionWithReturns(ifExpression: JetIfExpression) {
        val newReturnExpression = JetPsiFactory(ifExpression).createReturn(ifExpression)
        val newIfExpression = newReturnExpression.getReturnedExpression() as JetIfExpression

        val thenReturn = getFoldableBranchedReturn(newIfExpression.getThen()!!)!!
        val elseReturn = getFoldableBranchedReturn(newIfExpression.getElse()!!)!!

        val thenExpr = thenReturn.getReturnedExpression()!!
        val elseExpr = elseReturn.getReturnedExpression()!!

        thenReturn.replace(thenExpr)
        elseReturn.replace(elseExpr)

        ifExpression.replace(newReturnExpression)
    }

    public fun foldIfExpressionWithAsymmetricReturns(ifExpression: JetIfExpression) {
        val condition = ifExpression.getCondition()!!
        val thenRoot = ifExpression.getThen()!!
        val elseRoot = JetPsiUtil.skipTrailingWhitespacesAndComments(ifExpression) as JetExpression

        val psiFactory = JetPsiFactory(ifExpression)
        var newIfExpression = psiFactory.createIf(condition, thenRoot, elseRoot)
        val newReturnExpression = psiFactory.createReturn(newIfExpression)

        newIfExpression = newReturnExpression.getReturnedExpression() as JetIfExpression

        val thenReturn = getFoldableBranchedReturn(newIfExpression.getThen()!!)!!
        val elseReturn = getFoldableBranchedReturn(newIfExpression.getElse()!!)!!

        val thenExpr = thenReturn.getReturnedExpression()!!
        val elseExpr = elseReturn.getReturnedExpression()!!

        thenReturn.replace(thenExpr)
        elseReturn.replace(elseExpr)

        elseRoot.delete()
        ifExpression.replace(newReturnExpression)
    }

    public fun foldWhenExpressionWithAssignments(whenExpression: JetWhenExpression) {
        assert(!whenExpression.getEntries().isEmpty())

        val firstAssignment = getFoldableBranchedAssignment(whenExpression.getEntries().get(0).getExpression()!!)!!

        val op = firstAssignment.getOperationReference().getText()
        val lhs = firstAssignment.getLeft() as JetSimpleNameExpression

        val assignment = JetPsiFactory(whenExpression).createExpressionByPattern("$0 $1 $2", lhs, op, whenExpression)
        val newWhenExpression = (assignment as JetBinaryExpression).getRight() as JetWhenExpression

        for (entry in newWhenExpression.getEntries()) {
            val currAssignment = getFoldableBranchedAssignment(entry.getExpression()!!)!!
            val currRhs = currAssignment.getRight()!!
            currAssignment.replace(currRhs)
        }

        whenExpression.replace(assignment)
    }

    public fun foldWhenExpressionWithReturns(whenExpression: JetWhenExpression) {
        assert(!whenExpression.getEntries().isEmpty())

        val newReturnExpression = JetPsiFactory(whenExpression).createReturn(whenExpression)
        val newWhenExpression = newReturnExpression.getReturnedExpression() as JetWhenExpression

        for (entry in newWhenExpression.getEntries()) {
            val currReturn = getFoldableBranchedReturn(entry.getExpression()!!)!!
            val currExpr = currReturn.getReturnedExpression()!!
            currReturn.replace(currExpr)
        }

        whenExpression.replace(newReturnExpression)
    }
}
