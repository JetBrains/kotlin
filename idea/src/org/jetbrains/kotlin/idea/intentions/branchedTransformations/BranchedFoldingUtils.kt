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

import org.jetbrains.kotlin.cfg.WhenChecker
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.branches
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
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
                it.returnedExpression != null &&
                it.returnedExpression !is KtLambdaExpression &&
                it.getTargetLabel() == null
            }

    fun checkAssignmentsMatch(a1: KtBinaryExpression, a2: KtBinaryExpression): Boolean =
            a1.left?.text == a2.left?.text && a1.operationToken == a2.operationToken

    internal fun getFoldableAssignmentNumber(expression: KtExpression?): Int {
        expression ?: return -1
        val assignments = linkedSetOf<KtBinaryExpression>()
        fun collectAssignmentsAndCheck(e: KtExpression?): Boolean = when (e) {
            is KtWhenExpression -> {
                val entries = e.entries
                !e.hasMissingCases() &&
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
            is KtTryExpression -> {
                e.tryBlockAndCatchBodies().all {
                    val assignment = getFoldableBranchedAssignment(it)?.run { assignments.add(this) }
                    assignment != null || collectAssignmentsAndCheck(it?.lastBlockStatementOrThis())
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
        val firstAssignment = assignments.firstOrNull() ?: return 0
        if (assignments.any { !BranchedFoldingUtils.checkAssignmentsMatch(it, firstAssignment) }) {
            return -1
        }
        if (expression.anyDescendantOfType<KtBinaryExpression>(
                predicate = {
                    if (it.operationToken in KtTokens.ALL_ASSIGNMENTS)
                        if (it.getNonStrictParentOfType<KtFinallySection>() != null)
                            BranchedFoldingUtils.checkAssignmentsMatch(it, firstAssignment)
                        else
                            it !in assignments
                    else
                        false
                }
        )) {
            return -1
        }
        return assignments.size
    }

    private fun getFoldableReturns(branches: List<KtExpression?>): List<KtReturnExpression>? =
            branches.fold<KtExpression?, MutableList<KtReturnExpression>?>(mutableListOf()) { prevList, branch ->
                if (prevList == null) return@fold null
                val foldableBranchedReturn = getFoldableBranchedReturn(branch)
                if (foldableBranchedReturn != null) {
                    prevList.add(foldableBranchedReturn)
                }
                else {
                    val currReturns = getFoldableReturns(branch?.lastBlockStatementOrThis()) ?: return@fold null
                    prevList += currReturns
                }
                prevList
            }

    internal fun getFoldableReturns(expression: KtExpression?): List<KtReturnExpression>? = when (expression) {
        is KtWhenExpression -> {
            val entries = expression.entries
            when {
                expression.hasMissingCases() -> null
                entries.isEmpty() -> null
                else -> getFoldableReturns(entries.map { it.expression })
            }
        }
        is KtIfExpression -> {
            val branches = expression.branches
            when {
                branches.isEmpty() -> null
                branches.lastOrNull()?.getStrictParentOfType<KtIfExpression>()?.`else` == null -> null
                else -> getFoldableReturns(branches)
            }
        }
        is KtTryExpression -> {
            if (expression.finallyBlock?.finalExpression?.let { getFoldableReturns(listOf(it)) }?.isNotEmpty() == true)
                null
            else
                getFoldableReturns(expression.tryBlockAndCatchBodies())
        }
        is KtCallExpression -> {
            if (expression.analyze().getType(expression)?.isNothing() == true) emptyList() else null
        }
        is KtBreakExpression, is KtContinueExpression, is KtThrowExpression -> emptyList()
        else -> null
    }

    private fun getFoldableReturnNumber(expression: KtExpression?) = getFoldableReturns(expression)?.size ?: -1

    fun canFoldToReturn(expression: KtExpression?): Boolean = getFoldableReturnNumber(expression) > 0

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
                is KtTryExpression -> e.tryBlockAndCatchBodies().forEach {
                    getFoldableBranchedAssignment(it)?.replaceWithRHS() ?: lift(it?.lastBlockStatementOrThis())
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
                is KtTryExpression -> e.tryBlockAndCatchBodies().forEach {
                    getFoldableBranchedReturn(it)?.replaceWithReturned() ?:
                    lift(it?.lastBlockStatementOrThis())
                }
            }
        }
        lift(expression)
        expression.replace(KtPsiFactory(expression).createExpressionByPattern("return $0", expression))
    }

    private fun KtTryExpression.tryBlockAndCatchBodies(): List<KtExpression?> = listOf(tryBlock) + catchClauses.map { it.catchBody }

    private fun KtWhenExpression.hasMissingCases(): Boolean =
            !KtPsiUtil.checkWhenExpressionHasSingleElse(this) && WhenChecker.getMissingCases(this, this.analyze()).isNotEmpty()

}
