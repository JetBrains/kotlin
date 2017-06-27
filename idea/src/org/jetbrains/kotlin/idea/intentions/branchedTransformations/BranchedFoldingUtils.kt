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

    fun getFoldableBranchedReturn(branch: KtExpression?): KtReturnExpression? {
        return (branch?.lastBlockStatementOrThis() as? KtReturnExpression)?.takeIf { it.returnedExpression != null }
    }

    fun checkAssignmentsMatch(a1: KtBinaryExpression, a2: KtBinaryExpression): Boolean {
        return a1.left?.text == a2.left?.text && a1.operationToken == a2.operationToken
    }

    fun canFoldToAssignment(expression: KtExpression?): Boolean {
        val assignments = mutableListOf<KtBinaryExpression>()
        fun collectAssignmentsAndCheck(e: KtExpression?): Boolean {
            return when (e) {
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
        }
        if (!collectAssignmentsAndCheck(expression)) return false
        if (assignments.isEmpty()) return false
        val firstAssignment = assignments.first()
        return assignments.all { BranchedFoldingUtils.checkAssignmentsMatch(it, firstAssignment) }
    }

    fun canFoldToReturn(expression: KtExpression?): Boolean = when (expression) {
        is KtWhenExpression -> {
            val entries = expression.entries
            KtPsiUtil.checkWhenExpressionHasSingleElse(expression) &&
            entries.isNotEmpty() &&
            entries.all { entry ->
                getFoldableBranchedReturn(entry.expression) != null || canFoldToReturn(entry.expression?.lastBlockStatementOrThis())
            }
        }
        is KtIfExpression -> {
            val branches = expression.branches
            branches.isNotEmpty() &&
            (branches.lastOrNull()?.getStrictParentOfType<KtIfExpression>()?.`else` != null) &&
            branches.all { branch ->
                getFoldableBranchedReturn(branch) != null || canFoldToReturn(branch?.lastBlockStatementOrThis())
            }
        }
        is KtCallExpression -> {
            expression.analyze().getType(expression)?.isNothing() ?: false
        }
        is KtBreakExpression, is KtContinueExpression, is KtThrowExpression -> true
        else -> false
    }

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
