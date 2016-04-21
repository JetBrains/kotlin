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
import org.jetbrains.kotlin.idea.intentions.negate
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.blockExpressionsOrSingle

class FilterTransformation(
        override val loop: KtForExpression,
        val inputVariable: KtCallableDeclaration,
        val condition: KtExpression,
        val isInverse: Boolean
) : SequenceTransformation {

    fun effectiveCondition() = if (isInverse) condition.negate() else condition

    override fun mergeWithPrevious(previousTransformation: SequenceTransformation): SequenceTransformation? {
        if (previousTransformation !is FilterTransformation) return null
        assert(previousTransformation.inputVariable == inputVariable)
        val mergedCondition = KtPsiFactory(condition).createExpressionByPattern(
                "$0 && $1", previousTransformation.effectiveCondition(), effectiveCondition())
        return FilterTransformation(loop, inputVariable, mergedCondition, isInverse = false) //TODO: build filterNot in some cases?
    }

    private val functionName = if (isInverse) "filterNot" else "filter"

    override val affectsIndex: Boolean
        get() = true

    override val presentation: String
        get() = "$functionName{}"

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        val lambda = generateLambda(inputVariable, condition)
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
     */
    object Matcher : SequenceTransformationMatcher {
        override fun match(state: MatchingState): SequenceTransformationMatch? {
            val ifStatement = state.statements.firstOrNull() as? KtIfExpression ?: return null
            if (ifStatement.`else` != null) return null
            val condition = ifStatement.condition ?: return null
            val then = ifStatement.then ?: return null

            if (state.statements.size == 1) {
                val transformation = createFilterTransformation(state.outerLoop, state.inputVariable, state.indexVariable, condition, isInverse = false)
                val newState = state.copy(statements = listOf(then))
                return SequenceTransformationMatch(transformation, newState)
            }
            else {
                val statement = then.blockExpressionsOrSingle().singleOrNull() ?: return null
                when (statement) {
                    is KtContinueExpression -> {
                        if (statement.targetLoop() != state.innerLoop) return null
                        val transformation = createFilterTransformation(state.outerLoop, state.inputVariable, state.indexVariable, condition, isInverse = true)
                        val newState = state.copy(statements = state.statements.drop(1))
                        return SequenceTransformationMatch(transformation, newState)
                    }

                    is KtBreakExpression -> {
                        if (statement.targetLoop() != state.outerLoop) return null
                        val transformation = TakeWhileTransformation(state.outerLoop, state.inputVariable, condition.negate())
                        val newState = state.copy(statements = state.statements.drop(1))
                        return SequenceTransformationMatch(transformation, newState)
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
                return FilterIndexedTransformation(loop, inputVariable, indexVariable, effectiveCondition)
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

            return FilterTransformation(loop, inputVariable, condition, isInverse)
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

    override val affectsIndex: Boolean
        get() = true

    override val presentation: String
        get() = "filterNotNull()"

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        return chainedCallGenerator.generate("filterNotNull()")
    }
}

class FilterIndexedTransformation(
        override val loop: KtForExpression,
        val inputVariable: KtCallableDeclaration,
        val indexVariable: KtCallableDeclaration,
        val condition: KtExpression
) : SequenceTransformation {

    //TODO: how to handle multiple if's using index?

    override val affectsIndex: Boolean
        get() = true

    override val presentation: String
        get() = "filterIndexed{}"

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        val lambda = generateLambda(condition, indexVariable, inputVariable)
        return chainedCallGenerator.generate("filterIndexed $0:'{}'", lambda)
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
