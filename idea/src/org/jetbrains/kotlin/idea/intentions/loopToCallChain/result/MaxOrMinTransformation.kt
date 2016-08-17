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

package org.jetbrains.kotlin.idea.intentions.loopToCallChain.result

import org.jetbrains.kotlin.idea.intentions.loopToCallChain.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.blockExpressionsOrSingle

class MaxOrMinTransformation(
        loop: KtForExpression,
        private val inputVariable: KtCallableDeclaration,
        initialization: VariableInitialization,
        private val isMax: Boolean
) : AssignToVariableResultTransformation(loop, initialization) {

    override val presentation: String
        get() = if (isMax) "max()" else "min()"

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        val call = chainedCallGenerator.generate(presentation)
        return KtPsiFactory(call).createExpressionByPattern("$0\n ?: $1", call, initialization.initializer)
    }

    /**
     * Matches:
     *     val variable = <initial>
     *     for (...) {
     *         ...
     *         if (variable > <input variable>) { // or '<' or operands swapped
     *             variable = <input variable>
     *         }
     *     }
     */
    object Matcher : TransformationMatcher {
        override val indexVariableAllowed: Boolean
            get() = false

        override fun match(state: MatchingState): TransformationMatch.Result? {
            val ifExpression = state.statements.singleOrNull() as? KtIfExpression ?: return null
            if (ifExpression.`else` != null) return null

            val condition = ifExpression.condition as? KtBinaryExpression ?: return null
            val comparison = condition.operationToken
            if (comparison !in setOf(KtTokens.GT, KtTokens.LT)) return null
            val left = condition.left as? KtNameReferenceExpression ?: return null
            val right = condition.right as? KtNameReferenceExpression ?: return null
            val otherHand = if (left.isVariableReference(state.inputVariable)) {
                right
            }
            else if (right.isVariableReference(state.inputVariable)) {
                left
            }
            else {
                return null
            }

            val variableInitialization = (otherHand.isVariableInitializedBeforeLoop(state.outerLoop, checkNoOtherUsagesInLoop = false)
                                          ?: return null)

            val then = ifExpression.then ?: return null
            val statement = then.blockExpressionsOrSingle().singleOrNull() as? KtBinaryExpression ?: return null
            if (statement.operationToken != KtTokens.EQ) return null
            if (!statement.left.isVariableReference(variableInitialization.variable)) return null
            if (!statement.right.isVariableReference(state.inputVariable)) return null

            val isMax = (comparison == KtTokens.GT) xor (otherHand == left)
            val transformation = MaxOrMinTransformation(state.outerLoop, state.inputVariable, variableInitialization, isMax)
            return TransformationMatch.Result(transformation)
        }
    }
}