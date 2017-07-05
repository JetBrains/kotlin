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

import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.branches
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.lastBlockStatementOrThis
import org.jetbrains.kotlin.types.typeUtil.isNothing

object BranchedFoldingUtils {
    fun getFoldableBranchedAssignment(branch: KtExpression?): KtBinaryExpression? {
        fun checkAssignment(expression: KtBinaryExpression): Boolean {
            if (expression.operationToken !in KtTokens.ALL_ASSIGNMENTS) return false

            val left = expression.left as? KtNameReferenceExpression ?: return false
            if (expression.right == null) return false

            val parent = expression.parent
            if (parent is KtBlockExpression) {
                return !KtPsiUtil.checkVariableDeclarationInBlock(parent, left.text)
            }

            return true
        }
        return (branch?.lastBlockStatementOrThis() as? KtBinaryExpression)?.takeIf(::checkAssignment)
    }

    fun getFoldableBranchedReturn(branch: KtExpression?): KtReturnExpression? =
            (branch?.lastBlockStatementOrThis() as? KtReturnExpression)?.takeIf {
                it.returnedExpression != null && it.returnedExpression !is KtLambdaExpression
            }

    fun checkAssignmentsMatch(a1: KtBinaryExpression, a2: KtBinaryExpression): Boolean =
            a1.left?.text == a2.left?.text && a1.operationToken == a2.operationToken

    internal fun getFoldableAssignmentNumber(expression: KtExpression?): Int {
        expression ?: return -1
        val assignments = linkedSetOf<KtBinaryExpression>()
        fun collectAssignmentsAndCheck(e: KtExpression?): Boolean = when (e) {
            is KtWhenExpression -> {
                val entries = e.entries
                KtPsiUtil.checkWhenExpressionHasSingleElse(e) &&
                entries.isNotEmpty() &&
                entries.all { entry ->
                    val assignment = getFoldableBranchedAssignment(entry.expression)?.run { assignments.add(this) }
                    assignment != null || collectAssignmentsAndCheck(entry.expression?.lastBlockStatementOrThis())
                }
            }
            is KtIfExpression -> {
                val branches = e.branches
                branches.size > 1 &&
                (branches.lastOrNull()?.getStrictParentOfType<KtIfExpression>()?.`else` != null) &&
                branches.all { branch ->
                    val assignment = getFoldableBranchedAssignment(branch)?.run { assignments.add(this) }
                    assignment != null || collectAssignmentsAndCheck(branch?.lastBlockStatementOrThis())
                }
            }
            is KtCallExpression -> {
                e.analyze().getType(e)?.isNothing() ?: false
            }
            is KtBreakExpression, is KtContinueExpression,
            is KtThrowExpression, is KtReturnExpression -> true
            else -> false
        }
        if (!collectAssignmentsAndCheck(expression)) return -1
        val firstAssignment = assignments.firstOrNull()
        if (firstAssignment != null && assignments.any { !BranchedFoldingUtils.checkAssignmentsMatch(it, firstAssignment) }) {
            return -1
        }
        if (expression.anyDescendantOfType<KtBinaryExpression>(
                predicate = {
                    it.operationToken in KtTokens.ALL_ASSIGNMENTS && it !in assignments
                }
        )) {
            return -1
        }
        return assignments.size
    }

    private fun getFoldableReturnNumber(branches: List<KtExpression?>) =
            branches.fold(0) { prevNumber, branch ->
                when {
                    prevNumber == -1 -> -1
                    getFoldableBranchedReturn(branch) != null -> prevNumber + 1
                    else -> {
                        val currNumber = getFoldableReturnNumber(branch?.lastBlockStatementOrThis())
                        if (currNumber == -1) -1 else prevNumber + currNumber
                    }
                }
            }

    internal fun getFoldableReturnNumber(expression: KtExpression?): Int = when (expression) {
        is KtWhenExpression -> {
            val entries = expression.entries
            when {
                !KtPsiUtil.checkWhenExpressionHasSingleElse(expression) -> -1
                entries.isEmpty() -> -1
                else -> getFoldableReturnNumber(entries.map { it.expression })
            }
        }
        is KtIfExpression -> {
            val branches = expression.branches
            when {
                branches.isEmpty() -> -1
                branches.lastOrNull()?.getStrictParentOfType<KtIfExpression>()?.`else` == null -> -1
                else -> getFoldableReturnNumber(branches)
            }
        }
        is KtCallExpression -> {
            if (expression.analyze().getType(expression)?.isNothing() == true) 0 else -1
        }
        is KtBreakExpression, is KtContinueExpression, is KtThrowExpression -> 0
        else -> -1
    }

    fun canFoldToReturn(expression: KtExpression?): Boolean =
            getFoldableReturnNumber(expression) > 0

    fun foldToAssignment(expression: KtExpression) {
        var lhs: KtExpression? = null
        var op: String? = null
        fun KtBinaryExpression.replaceWithRHS() {
            if (lhs == null || op == null) {
                lhs = left!!.copy() as KtExpression
                op = operationReference.text
            }
            replace(right!!)
        }
        fun lift(e: KtExpression?) {
            when (e) {
                is KtWhenExpression -> e.entries.forEach { entry ->
                    getFoldableBranchedAssignment(entry.expression)?.replaceWithRHS() ?: lift(entry.expression?.lastBlockStatementOrThis())
                }
                is KtIfExpression -> e.branches.forEach { branch ->
                    getFoldableBranchedAssignment(branch)?.replaceWithRHS() ?: lift(branch?.lastBlockStatementOrThis())
                }
            }
        }
        lift(expression)
        expression.replace(KtPsiFactory(expression).createExpressionByPattern("$0 $1 $2", lhs!!, op!!, expression))
    }

    fun foldToReturn(expression: KtExpression) {
        fun KtReturnExpression.replaceWithReturned() {
            replace(returnedExpression!!)
        }
        fun lift(e: KtExpression?) {
            when (e) {
                is KtWhenExpression -> e.entries.forEach { entry ->
                    getFoldableBranchedReturn(entry.expression)?.replaceWithReturned()
                    ?: lift(entry.expression?.lastBlockStatementOrThis())
                }
                is KtIfExpression -> e.branches.forEach { branch ->
                    getFoldableBranchedReturn(branch)?.replaceWithReturned() ?:
                    lift(branch?.lastBlockStatementOrThis())
                }
            }
        }
        lift(expression)
        expression.replace(KtPsiFactory(expression).createExpressionByPattern("return $0", expression))
    }
}
