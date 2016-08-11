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

import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isNullExpression
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.*
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.result.FindTransformationMatcher
import org.jetbrains.kotlin.idea.intentions.negate
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.blockExpressionsOrSingle

class FilterTransformation(
        override val loop: KtForExpression,
        val inputVariable: KtCallableDeclaration,
        val indexVariable: KtCallableDeclaration?,
        val condition: KtExpression,
        val isInverse: Boolean
) : SequenceTransformation {

    fun effectiveCondition() = if (isInverse) condition.negate() else condition

    private val functionName = when {
        indexVariable != null -> "filterIndexed"
        isInverse -> "filterNot"
        else -> "filter"
    }

    override val affectsIndex: Boolean
        get() = true

    override val presentation: String
        get() = "$functionName{}"

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        val lambda = if (indexVariable != null)
            generateLambda(inputVariable, indexVariable, effectiveCondition())
        else
            generateLambda(inputVariable, condition)
        return chainedCallGenerator.generate("$0$1:'{}'", functionName, lambda)
    }

    /**
     * Matches:
     *     for (...) {
     *         if (<condition>) {
     *             ...
     *         }
     *     }
     *
     * or
     *
     *     for (...) {
     *         if (<condition>) continue
     *         ...
     *     }
     * or
     *
     *     for (...) {
     *         if (<condition>) break
     *         ...
     *     }
     *
     * plus optionally consequent find operation (assignment or return)
     */
    object Matcher : TransformationMatcher {
        override val indexVariableAllowed: Boolean
            get() = true

        override fun match(state: MatchingState): TransformationMatch? {
            // we merge filter transformations here instead of FilterTransformation.mergeWithPrevious() because of filterIndexed that won't merge otherwise

            var (transformation, currentState) = matchOneTransformation(state) ?: return null
            assert(currentState.indexVariable == state.indexVariable) // indexVariable should not change

            if (transformation is FilterTransformation) {
                while (true) {
                    currentState = currentState.unwrapBlock()

                    val (nextTransformation, nextState) = matchOneTransformation(currentState) ?: break
                    if (nextTransformation !is FilterTransformation) break
                    assert(nextState.indexVariable == currentState.indexVariable) // indexVariable should not change

                    val indexVariable = transformation.indexVariable ?: nextTransformation.indexVariable
                    val mergedCondition = KtPsiFactory(state.outerLoop).createExpressionByPattern(
                            "$0 && $1", transformation.effectiveCondition(), nextTransformation.effectiveCondition())
                    transformation = FilterTransformation(state.outerLoop, transformation.inputVariable, indexVariable, mergedCondition, isInverse = false) //TODO: build filterNot in some cases?
                    currentState = nextState
                }

                FindTransformationMatcher.matchWithFilterBefore(currentState, transformation)
                        ?.let { return it }
            }

            return TransformationMatch.Sequence(transformation, currentState)
        }

        private fun matchOneTransformation(state: MatchingState): Pair<SequenceTransformation, MatchingState>? {
            val ifStatement = state.statements.firstOrNull() as? KtIfExpression ?: return null
            if (ifStatement.`else` != null) return null
            val condition = ifStatement.condition ?: return null
            val then = ifStatement.then ?: return null

            if (state.statements.size == 1) {
                val transformation = createFilterTransformation(state.outerLoop, state.inputVariable, state.indexVariable, condition, isInverse = false)
                val newState = state.copy(statements = listOf(then))
                return transformation to newState
            }
            else {
                val statement = then.blockExpressionsOrSingle().singleOrNull() ?: return null
                when (statement) {
                    is KtContinueExpression -> {
                        if (statement.targetLoop() != state.innerLoop) return null
                        val transformation = createFilterTransformation(state.outerLoop, state.inputVariable, state.indexVariable, condition, isInverse = true)
                        val newState = state.copy(statements = state.statements.drop(1))
                        return transformation to newState
                    }

                    is KtBreakExpression -> {
                        if (statement.targetLoop() != state.outerLoop) return null
                        val transformation = TakeWhileTransformation(state.outerLoop, state.inputVariable, condition.negate())
                        val newState = state.copy(statements = state.statements.drop(1))
                        return transformation to newState
                    }

                    else -> return null
                }
            }
        }

        //TODO: choose filter or filterNot depending on condition
        private fun createFilterTransformation(
                loop: KtForExpression,
                inputVariable: KtCallableDeclaration,
                indexVariable: KtCallableDeclaration?,
                condition: KtExpression,
                isInverse: Boolean
        ): SequenceTransformation {

            val effectiveCondition = if (isInverse) condition.negate() else condition

            if (indexVariable != null && indexVariable.hasUsages(condition)) {
                return FilterTransformation(loop, inputVariable, indexVariable, effectiveCondition, isInverse = false)
            }

            if (effectiveCondition is KtIsExpression
                && !effectiveCondition.isNegated
                && effectiveCondition.leftHandSide.isSimpleName(inputVariable.nameAsSafeName) // we cannot use isVariableReference here because expression can be non-physical
            ) {
                val typeRef = effectiveCondition.typeReference
                if (typeRef != null) {
                    return FilterIsInstanceTransformation(loop, typeRef)
                }
            }

            if (effectiveCondition is KtBinaryExpression
                && effectiveCondition.operationToken == KtTokens.EXCLEQ
                && effectiveCondition.right.isNullExpression()
                && effectiveCondition.left.isSimpleName(inputVariable.nameAsSafeName)
            ) {
                return FilterNotNullTransformation(loop)
            }

            return FilterTransformation(loop, inputVariable, null, condition, isInverse)
        }
    }
}

class FilterIsInstanceTransformation(
        override val loop: KtForExpression,
        private val type: KtTypeReference
) : SequenceTransformation {

    override val affectsIndex: Boolean
        get() = true

    override val presentation: String
        get() = "filterIsInstance<>()"

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        return chainedCallGenerator.generate("filterIsInstance<$0>()", type)
    }
}

class FilterNotNullTransformation(override val loop: KtForExpression) : SequenceTransformation {
    override fun mergeWithPrevious(previousTransformation: SequenceTransformation): SequenceTransformation? {
        if (previousTransformation is MapTransformation) {
            return MapTransformation(loop, previousTransformation.inputVariable, previousTransformation.indexVariable, previousTransformation.mapping, mapNotNull = true)
        }
        return null
    }

    override val affectsIndex: Boolean
        get() = true

    override val presentation: String
        get() = "filterNotNull()"

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        return chainedCallGenerator.generate("filterNotNull()")
    }
}

class TakeWhileTransformation(
        override val loop: KtForExpression,
        val inputVariable: KtCallableDeclaration,
        val condition: KtExpression
) : SequenceTransformation {

    //TODO: merge multiple

    override val affectsIndex: Boolean
        get() = false

    override val presentation: String
        get() = "takeWhile{}"

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        val lambda = generateLambda(inputVariable, condition)
        return chainedCallGenerator.generate("takeWhile$0:'{}'", lambda)
    }
}
