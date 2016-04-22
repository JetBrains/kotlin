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

import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isNullExpression
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.*
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.sequence.FilterTransformation
import org.jetbrains.kotlin.idea.intentions.negate
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange
import org.jetbrains.kotlin.types.typeUtil.TypeNullability
import org.jetbrains.kotlin.types.typeUtil.nullability
import org.jetbrains.kotlin.utils.addToStdlib.check

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
 * or
 *     for (...) {
 *         ...
 *         return ...
 *     }
 *     return ...
 */
object FindTransformationMatcher : TransformationMatcher {
    override val indexVariableAllowed: Boolean
        get() = false

    override fun match(state: MatchingState): TransformationMatch.Result? {
        return matchWithFilterBefore(state, null)
    }

    fun matchWithFilterBefore(state: MatchingState, filterTransformation: FilterTransformation?): TransformationMatch.Result? {
        matchReturn(state, filterTransformation)?.let { return it }

        when (state.statements.size) {
            1 -> { }

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

        val generator = buildFindOperationGenerator(state.outerLoop, right, initialization.initializer, state.inputVariable,
                                                    state.indexVariable, filterTransformation, findFirst)
                        ?: return null

        val transformation = FindAndAssignTransformation(state.outerLoop, generator, initialization)
        return TransformationMatch.Result(transformation)
    }

    private fun matchReturn(state: MatchingState, filterTransformation: FilterTransformation?): TransformationMatch.Result? {
        val returnInLoop = state.statements.singleOrNull() as? KtReturnExpression ?: return null
        val returnAfterLoop = state.outerLoop.nextStatement() as? KtReturnExpression ?: return null
        if (returnInLoop.getLabelName() != returnAfterLoop.getLabelName()) return null

        val returnValueInLoop = returnInLoop.returnedExpression ?: return null
        val returnValueAfterLoop = returnAfterLoop.returnedExpression ?: return null

        val generator = buildFindOperationGenerator(state.outerLoop, returnValueInLoop, returnValueAfterLoop,
                                                    state.inputVariable, state.indexVariable, filterTransformation, findFirst = true)
                        ?: return null

        val transformation = FindAndReturnTransformation(state.outerLoop, generator, returnAfterLoop)
        return TransformationMatch.Result(transformation)
    }

    private class FindAndReturnTransformation(
            override val loop: KtForExpression,
            private val generator: FindOperationGenerator,
            private val endReturn: KtReturnExpression
    ) : ResultTransformation {

        override val commentSavingRange = PsiChildRange(loop.unwrapIfLabeled(), endReturn)

        override val presentation: String
            get() = generator.presentation

        override val chainCallCount: Int
            get() = generator.chainCallCount

        override val shouldUseInputVariable: Boolean
            get() = generator.shouldUseInputVariable

        override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
            return generator.generate(chainedCallGenerator)
        }

        override val expressionToBeReplacedByResultCallChain: KtExpression
            get() = endReturn.returnedExpression!!

        override fun convertLoop(resultCallChain: KtExpression, commentSavingRangeHolder: CommentSavingRangeHolder): KtExpression {
            endReturn.returnedExpression!!.replace(resultCallChain)
            loop.deleteWithLabels()
            return endReturn
        }
    }

    private class FindAndAssignTransformation(
            loop: KtForExpression,
            private val generator: FindOperationGenerator,
            initialization: VariableInitialization
    ) : AssignToVariableResultTransformation(loop, initialization) {

        override val presentation: String
            get() = generator.presentation

        override val chainCallCount: Int
            get() = generator.chainCallCount

        override val shouldUseInputVariable: Boolean
            get() = generator.shouldUseInputVariable

        override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
            return generator.generate(chainedCallGenerator)
        }
    }

    private interface FindOperationGenerator {
        val functionName: String

        val hasFilter: Boolean

        val presentation: String
            get() = functionName + (if (hasFilter) "{}" else "()")

        val shouldUseInputVariable: Boolean

        fun generate(chainedCallGenerator: ChainedCallGenerator): KtExpression

        val chainCallCount: Int
            get() = 1
    }

    private class SimpleGenerator(
            override val functionName: String,
            private val inputVariable: KtCallableDeclaration,
            private val filter: KtExpression?,
            override val shouldUseInputVariable: Boolean
    ) : FindOperationGenerator {
        override val hasFilter: Boolean
            get() = filter != null

        override fun generate(chainedCallGenerator: ChainedCallGenerator): KtExpression {
            return generateChainedCall(functionName, chainedCallGenerator, inputVariable, filter)
        }
    }

    private fun generateChainedCall(
            stdlibFunName: String,
            chainedCallGenerator: ChainedCallGenerator,
            inputVariable: KtCallableDeclaration,
            filter: KtExpression?
    ): KtExpression {
        return if (filter == null) {
            chainedCallGenerator.generate("$stdlibFunName()")
        }
        else {
            val lambda = generateLambda(inputVariable, filter)
            chainedCallGenerator.generate("$stdlibFunName $0:'{}'", lambda)
        }
    }

    private fun buildFindOperationGenerator(
            loop: KtForExpression,
            valueIfFound: KtExpression,
            valueIfNotFound: KtExpression,
            inputVariable: KtCallableDeclaration,
            indexVariable: KtCallableDeclaration?,
            filterTransformation: FilterTransformation?,
            findFirst: Boolean
    ): FindOperationGenerator?  {
        assert(valueIfFound.isPhysical)
        assert(valueIfNotFound.isPhysical)

        val filter = filterTransformation?.effectiveCondition()

        if (indexVariable != null) {
            if (filterTransformation == null) return null // makes no sense, indexVariable must be always 0
            if (filterTransformation.indexVariable != null) return null // cannot use index in condition for indexOfFirst/indexOfLast

            //TODO: what if value when not found is not "-1"?
            if (valueIfFound.isVariableReference(indexVariable) && valueIfNotFound.text == "-1") {
                val functionName = if (findFirst) "indexOfFirst" else "indexOfLast"
                return SimpleGenerator(functionName, inputVariable, filter, shouldUseInputVariable = false)
            }

            return null

        }
        else {
            val inputVariableCanHoldNull = (inputVariable.resolveToDescriptor() as VariableDescriptor).type.nullability() != TypeNullability.NOT_NULL

            fun FindOperationGenerator.useElvisOperatorIfNeeded(): FindOperationGenerator? {
                if (valueIfNotFound.isNullExpression()) return this

                // we cannot use ?: if found value can be null
                if (inputVariableCanHoldNull) return null

                return object: FindOperationGenerator by this {
                    override fun generate(chainedCallGenerator: ChainedCallGenerator): KtExpression {
                        val generated = this@useElvisOperatorIfNeeded.generate(chainedCallGenerator)
                        return KtPsiFactory(generated).createExpressionByPattern("$0 ?: $1", generated, valueIfNotFound)
                    }
                }
            }

            when {
                valueIfFound.isVariableReference(inputVariable) -> {
                    val functionName = if (findFirst) "firstOrNull" else "lastOrNull"
                    val generator = SimpleGenerator(functionName, inputVariable, filter, shouldUseInputVariable = true)
                    return generator.useElvisOperatorIfNeeded()
                }

                valueIfFound.isTrueConstant() && valueIfNotFound.isFalseConstant() -> {
                    return buildFoundFlagGenerator(loop, inputVariable, filter, negated = false)
                }

                valueIfFound.isFalseConstant() && valueIfNotFound.isTrueConstant() -> {
                    return buildFoundFlagGenerator(loop, inputVariable, filter, negated = true)
                }

                inputVariable.hasUsages(valueIfFound) -> {
                    if (!findFirst) return null // too dangerous because of side effects

                    // specially handle the case when the result expression is "<input variable>.<some call>" or "<input variable>?.<some call>"
                    val qualifiedExpression = valueIfFound as? KtQualifiedExpression
                    if (qualifiedExpression != null) {
                        val receiver = qualifiedExpression.receiverExpression
                        val selector = qualifiedExpression.selectorExpression
                        if (receiver.isVariableReference(inputVariable) && selector != null && !inputVariable.hasUsages(selector)) {
                            return object: FindOperationGenerator {
                                override val functionName: String
                                    get() = "firstOrNull"

                                override val hasFilter: Boolean
                                    get() = filter != null

                                override val shouldUseInputVariable: Boolean
                                    get() = true

                                override val chainCallCount: Int
                                    get() = 2

                                override fun generate(chainedCallGenerator: ChainedCallGenerator): KtExpression {
                                    val findFirstCall = generateChainedCall(functionName, chainedCallGenerator, inputVariable, filter)
                                    return chainedCallGenerator.generate("$0", selector, receiver = findFirstCall, safeCall = true)
                                }
                            }.useElvisOperatorIfNeeded()
                        }
                    }

                    // in case of nullable input variable we cannot distinguish by the result of "firstOrNull" whether nothing was found or 'null' was found
                    if (inputVariableCanHoldNull) return null

                    return object: FindOperationGenerator {
                        override val functionName: String
                            get() = "firstOrNull"

                        override val hasFilter: Boolean
                            get() = filter != null

                        override val shouldUseInputVariable: Boolean
                            get() = true

                        override val chainCallCount: Int
                            get() = 2 // also includes "let"

                        override fun generate(chainedCallGenerator: ChainedCallGenerator): KtExpression {
                            val findFirstCall = generateChainedCall(functionName, chainedCallGenerator, inputVariable, filter)
                            val letBody = generateLambda(inputVariable, valueIfFound)
                            return chainedCallGenerator.generate("let $0:'{}'", letBody, receiver = findFirstCall, safeCall = true)
                        }
                    }.useElvisOperatorIfNeeded()
                }

                else -> {
                    val generator = buildFoundFlagGenerator(loop, inputVariable, filter, negated = false)
                    return object: FindOperationGenerator by generator {
                        override fun generate(chainedCallGenerator: ChainedCallGenerator): KtExpression {
                            val chainedCall = generator.generate(chainedCallGenerator)
                            return KtPsiFactory(chainedCall).createExpressionByPattern("if ($0) $1 else $2", chainedCall, valueIfFound, valueIfNotFound)
                        }
                    }
                }
            }
        }
    }

    private fun buildFoundFlagGenerator(
            loop: KtForExpression,
            inputVariable: KtCallableDeclaration,
            filter: KtExpression?,
            negated: Boolean
    ): FindOperationGenerator {
        if (filter != null) {
            val containsArgument = filter.isFilterForContainsOperation(inputVariable, loop)
            if (containsArgument != null) {
                return object: FindOperationGenerator {
                    override val functionName: String
                        get() = "contains"

                    override val hasFilter: Boolean
                        get() = false

                    override val shouldUseInputVariable: Boolean
                        get() = false

                    override fun generate(chainedCallGenerator: ChainedCallGenerator): KtExpression {
                        val expression = chainedCallGenerator.generate("contains($0)", containsArgument)
                        return if (negated) expression.negate() else expression
                    }
                }
            }
        }

        if (negated) {
            return SimpleGenerator("none", inputVariable, filter, shouldUseInputVariable = false)
        }
        else {
            return SimpleGenerator("any", inputVariable, filter, shouldUseInputVariable = false)
        }
    }

    private fun KtExpression.isFilterForContainsOperation(inputVariable: KtCallableDeclaration, loop: KtForExpression): KtExpression? {
        if (this !is KtBinaryExpression) return null
        if (operationToken != KtTokens.EQEQ) return null
        return when {
            left.isVariableReference(inputVariable) -> right?.check { it.isStableInLoop(loop, false) }
            right.isVariableReference(inputVariable) -> left?.check { it.isStableInLoop(loop, false) }
            else -> null
        }
    }
}
