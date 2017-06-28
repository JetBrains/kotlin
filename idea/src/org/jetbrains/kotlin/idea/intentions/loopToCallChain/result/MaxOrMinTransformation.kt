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
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.sequence.MapTransformation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.blockExpressionsOrSingle

class MaxOrMinTransformation(
        loop: KtForExpression,
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
     *
     *     or
     *
     *     val variable = <initial>
     *     for (...) {
     *         ...
     *         // or '<', '<=', '>=' or operands swapped
     *         variable = if (variable > <input variable>) <input variable> else variable
     *         }
     *     }

     *     or
     *
     *     val variable = <initial>
     *     for (...) {
     *         ...
     *         // or Math.min or operands swapped
     *         variable = Math.max(variable, <expression>)
     *         }
     *     }
     */
    object Matcher : TransformationMatcher {
        override val indexVariableAllowed: Boolean
            get() = true

        override fun match(state: MatchingState): TransformationMatch.Result? {
            return matchIfAssign(state)
                   ?: matchAssignIf(state)
                   ?: matchMathMaxOrMin(state)
        }

        private fun matchIfAssign(state: MatchingState): TransformationMatch.Result? {
            val ifExpression = state.statements.singleOrNull() as? KtIfExpression ?: return null
            if (ifExpression.`else` != null) return null

            val then = ifExpression.then ?: return null
            val statement = then.blockExpressionsOrSingle().singleOrNull() as? KtBinaryExpression ?: return null
            if (statement.operationToken != KtTokens.EQ) return null

            return match(ifExpression.condition, statement.left, statement.right, null, state.inputVariable, state.outerLoop)
        }

        private fun matchAssignIf(state: MatchingState): TransformationMatch.Result? {
            val assignment = state.statements.singleOrNull() as? KtBinaryExpression ?: return null
            if (assignment.operationToken != KtTokens.EQ) return null

            val ifExpression = assignment.right as? KtIfExpression ?: return null

            return match(ifExpression.condition, assignment.left, ifExpression.then, ifExpression.`else`, state.inputVariable, state.outerLoop)
        }

        private fun matchMathMaxOrMin(state: MatchingState): TransformationMatch.Result? {
            val assignment = state.statements.singleOrNull() as? KtBinaryExpression ?: return null
            if (assignment.operationToken != KtTokens.EQ) return null

            val variableInitialization = assignment.left.findVariableInitializationBeforeLoop(state.outerLoop, checkNoOtherUsagesInLoop = false)
                                         ?: return null

            return matchMathMaxOrMin(variableInitialization, assignment, state, isMax = true)
                   ?: matchMathMaxOrMin(variableInitialization, assignment, state, isMax = false)
        }

        private fun matchMathMaxOrMin(
                variableInitialization: VariableInitialization,
                assignment: KtBinaryExpression,
                state: MatchingState,
                isMax: Boolean
        ): TransformationMatch.Result? {
            val functionName = if (isMax) "max" else "min"
            val arguments = assignment.right.extractStaticFunctionCallArguments("java.lang.Math." + functionName) ?: return null
            if (arguments.size != 2) return null
            val value = when {
                arguments[0].isVariableReference(variableInitialization.variable) -> arguments[1] ?: return null
                arguments[1].isVariableReference(variableInitialization.variable) -> arguments[0] ?: return null
                else -> return null
            }

            val mapTransformation = if (value.isVariableReference(state.inputVariable))
                null
            else
                MapTransformation(state.outerLoop, state.inputVariable, state.indexVariable, value, mapNotNull = false)

            val transformation = MaxOrMinTransformation(state.outerLoop, variableInitialization, isMax)
            return TransformationMatch.Result(transformation, listOfNotNull(mapTransformation))
        }

        private fun match(
                condition: KtExpression?,
                assignmentTarget: KtExpression?,
                valueAssignedIfTrue: KtExpression?,
                valueAssignedIfFalse: KtExpression?,
                inputVariable: KtCallableDeclaration,
                loop: KtForExpression
        ): TransformationMatch.Result? {
            if (condition !is KtBinaryExpression) return null
            val comparison = condition.operationToken
            if (comparison !in setOf(KtTokens.GT, KtTokens.LT, KtTokens.GTEQ, KtTokens.LTEQ)) return null
            val left = condition.left as? KtNameReferenceExpression ?: return null
            val right = condition.right as? KtNameReferenceExpression ?: return null
            val otherHand = when {
                left.isVariableReference(inputVariable) -> right
                right.isVariableReference(inputVariable) -> left
                else -> return null
            }

            val variableInitialization = otherHand.findVariableInitializationBeforeLoop(loop, checkNoOtherUsagesInLoop = false)
                                         ?: return null

            if (!assignmentTarget.isVariableReference(variableInitialization.variable)) return null

            val valueToBeVariable = when {
                valueAssignedIfTrue.isVariableReference(inputVariable) -> valueAssignedIfFalse
                valueAssignedIfFalse.isVariableReference(inputVariable) -> valueAssignedIfTrue
                else -> return null
            }
            if (valueToBeVariable != null && !valueToBeVariable.isVariableReference(variableInitialization.variable)) return null

            val isMax = (comparison == KtTokens.GT || comparison == KtTokens.GTEQ) xor (otherHand == left)
            val transformation = MaxOrMinTransformation(loop, variableInitialization, isMax)
            return TransformationMatch.Result(transformation)
        }
    }
}