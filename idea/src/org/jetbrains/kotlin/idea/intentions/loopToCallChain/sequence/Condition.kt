/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.intentions.loopToCallChain.sequence

import org.jetbrains.kotlin.idea.intentions.loopToCallChain.hasUsages
import org.jetbrains.kotlin.idea.intentions.negate
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

interface Condition {
    fun asExpression(): KtExpression
    fun asNegatedExpression(): KtExpression
    fun toAtomicConditions(): List<AtomicCondition>

    companion object {
        fun create(expression: KtExpression, negated: Boolean = false): Condition {
            if (negated) {
                if (expression is KtBinaryExpression && expression.operationToken == KtTokens.OROR) {
                    //TODO: check Boolean type for operands
                    val left = expression.left
                    val right = expression.right
                    if (left != null && right != null) {
                        val leftCondition = create(left, negated = true)
                        val rightCondition = create(right, negated = true)
                        return CompositeCondition.create(leftCondition.toAtomicConditions() + rightCondition.toAtomicConditions())
                    }
                }
            }
            else {
                if (expression is KtBinaryExpression && expression.operationToken == KtTokens.ANDAND) {
                    //TODO: check Boolean type for operands
                    val left = expression.left
                    val right = expression.right
                    if (left != null && right != null) {
                        val leftCondition = create(left)
                        val rightCondition = create(right)
                        return CompositeCondition.create(leftCondition.toAtomicConditions() + rightCondition.toAtomicConditions())
                    }
                }
            }
            return AtomicCondition(expression, negated)
        }
    }
}

class AtomicCondition(val expression: KtExpression, private val isNegated: Boolean = false) : Condition {
    init {
        assert(expression.isPhysical)
    }

    override fun asExpression() = if (isNegated) expression.negate() else expression
    override fun asNegatedExpression() = if (isNegated) expression else expression.negate()
    override fun toAtomicConditions() = listOf(this)

    fun negate() = AtomicCondition(expression, !isNegated)
}

class CompositeCondition private constructor(val conditions: List<AtomicCondition>) : Condition {
    override fun asExpression(): KtExpression {
        val factory = KtPsiFactory(conditions.first().expression)
        return factory.buildExpression {
            for ((index, condition) in conditions.withIndex()) {
                if (index > 0) {
                    appendFixedText("&&")
                }
                appendExpression(condition.asExpression())
            }
        }
    }

    override fun asNegatedExpression(): KtExpression {
        return asExpression().negate()
    }

    override fun toAtomicConditions() = conditions

    companion object {
        fun create(conditions: List<AtomicCondition>): Condition {
            return conditions.singleOrNull() ?: CompositeCondition(conditions)
        }
    }
}

fun Condition.hasUsagesOf(variable: KtCallableDeclaration): Boolean {
    return toAtomicConditions().any { variable.hasUsages(it.expression) }
}

