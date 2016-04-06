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
        override val inputVariable: KtCallableDeclaration,
        val condition: KtExpression,
        val isInverse: Boolean
) : SequenceTransformation {

    fun effectiveCondition() = if (isInverse) condition.negate() else condition

    override fun mergeWithPrevious(previousTransformation: SequenceTransformation): SequenceTransformation? {
        if (previousTransformation !is FilterTransformation) return null
        assert(previousTransformation.inputVariable == inputVariable)
        val mergedCondition = KtPsiFactory(condition).createExpressionByPattern(
                "$0 && $1", previousTransformation.effectiveCondition(), effectiveCondition())
        return FilterTransformation(inputVariable, mergedCondition, isInverse = false) //TODO: build filterNot in some cases?
    }

    override val affectsIndex: Boolean
        get() = true

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        val lambda = generateLambda(inputVariable, condition)
        val name = if (isInverse) "filterNot" else "filter"
        return chainedCallGenerator.generate("$0$1:'{}'", name, lambda)
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
     */
    object Matcher : SequenceTransformationMatcher {
        override fun match(state: MatchingState): SequenceTransformationMatch? {
            if (state.indexVariable != null) return null //TODO?

            val ifStatement = state.statements.firstOrNull() as? KtIfExpression ?: return null
            if (ifStatement.`else` != null) return null
            val condition = ifStatement.condition ?: return null
            val then = ifStatement.then ?: return null

            if (state.statements.size == 1) {
                val transformation = createFilterTransformation(state.inputVariable, condition, isInverse = false)
                val newState = state.copy(statements = listOf(then))
                return SequenceTransformationMatch(transformation, newState)
            }
            else {
                val continueExpression = then.blockExpressionsOrSingle().singleOrNull() as? KtContinueExpression ?: return null
                if (!continueExpression.isBreakOrContinueOfLoop(state.innerLoop)) return null
                val transformation = createFilterTransformation(state.inputVariable, condition, isInverse = true)
                val newState = state.copy(statements = state.statements.drop(1))
                return SequenceTransformationMatch(transformation, newState)
            }
        }

        //TODO: choose filter or filterNot depending on condition
        private fun createFilterTransformation(
                inputVariable: KtCallableDeclaration,
                condition: KtExpression,
                isInverse: Boolean): SequenceTransformation {

            val effectiveCondition = if (isInverse) condition.negate() else condition

            if (effectiveCondition is KtIsExpression
                && !effectiveCondition.isNegated
                && effectiveCondition.leftHandSide.isSimpleName(inputVariable.nameAsSafeName) // we cannot use isVariableReference here because expression can be non-physical
            ) {
                val typeRef = effectiveCondition.typeReference
                if (typeRef != null) {
                    return FilterIsInstanceTransformation(inputVariable, typeRef)
                }
            }

            if (effectiveCondition is KtBinaryExpression
                && effectiveCondition.operationToken == KtTokens.EXCLEQ
                && effectiveCondition.right.isNullExpression()
                && effectiveCondition.left.isSimpleName(inputVariable.nameAsSafeName)
            ) {
                return FilterNotNullTransformation(inputVariable)
            }

            return FilterTransformation(inputVariable, condition, isInverse)
        }
    }
}

class FilterIsInstanceTransformation(
        override val inputVariable: KtCallableDeclaration,
        private val type: KtTypeReference
) : SequenceTransformation {

    override val affectsIndex: Boolean
        get() = true

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        return chainedCallGenerator.generate("filterIsInstance<$0>()", type)
    }
}

class FilterNotNullTransformation(
        override val inputVariable: KtCallableDeclaration
) : SequenceTransformation {

    override val affectsIndex: Boolean
        get() = true

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        return chainedCallGenerator.generate("filterNotNull()")
    }
}
