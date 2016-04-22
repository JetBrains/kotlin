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
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.sequence.FilterTransformation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBreakExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtForExpression

class FindAndAssignTransformation(
        loop: KtForExpression,
        private val generator: FindOperatorGenerator,
        initialization: VariableInitialization,
        private val filter: KtExpression? = null
) : AssignToVariableResultTransformation(loop, initialization) {

    override fun mergeWithPrevious(previousTransformation: SequenceTransformation): ResultTransformation? {
        if (previousTransformation !is FilterTransformation) return null
        if (previousTransformation.indexVariable != null) return null
        assert(filter == null) { "Should not happen because no 2 consecutive FilterTransformation's possible"}
        return FindAndAssignTransformation(loop, generator, initialization, previousTransformation.effectiveCondition())
    }

    override val presentation: String
        get() = generator.functionName + (if (filter != null) "{}" else "()")

    override val chainCallCount: Int
        get() = generator.chainCallCount

    override val shouldUseInputVariable: Boolean
        get() = generator.shouldUseInputVariable

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        return generator.generate(chainedCallGenerator, filter)
    }

    /**
     * Matches:
     *     val variable = ...
     *     for (...) {
     *         ...
     *         variable = ...
     *         break
     *     }
     * or
     *     val variable = ...
     *     for (...) {
     *         ...
     *         variable = ...
     *     }
     */
    object Matcher : ResultTransformationMatcher {
        override val indexVariableUsePossible: Boolean
            get() = false

        override fun match(state: MatchingState): ResultTransformationMatch? {
            when (state.statements.size) {
                1 -> {}

                2 -> {
                    val breakExpression = state.statements.last() as? KtBreakExpression ?: return null
                    if (breakExpression.targetLoop() != state.outerLoop) return null
                }

                else -> return null
            }
            val findFirst = state.statements.size == 2

            val binaryExpression = state.statements.first() as? KtBinaryExpression ?: return null
            if (binaryExpression.operationToken != KtTokens.EQ) return null
            val left = binaryExpression.left ?: return null
            val right = binaryExpression.right ?: return null

            val initialization = left.detectInitializationBeforeLoop(state.outerLoop, checkNoOtherUsagesInLoop = true) ?: return null

            if (initialization.variable.countUsages(state.outerLoop) != 1) return null // this should be the only usage of this variable inside the loop

            // we do not try to convert anything if the initializer is not compile-time constant because of possible side-effects
            if (!initialization.initializer.isConstant()) return null

            val generator = buildFindOperationGenerator(right, initialization.initializer, state.inputVariable, findFirst) ?: return null

            val transformation = FindAndAssignTransformation(state.outerLoop, generator, initialization)
            return ResultTransformationMatch(transformation)
        }
    }
}