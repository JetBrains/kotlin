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
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.sequence.FlatMapTransformation
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.sequence.MapTransformation
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression

class AddToCollectionTransformation(
        loop: KtForExpression,
        inputVariable: KtCallableDeclaration,
        private val targetCollection: KtExpression
) : ReplaceLoopResultTransformation(loop, inputVariable) {

    override fun mergeWithPrevious(previousTransformation: SequenceTransformation): ResultTransformation? {
        return when (previousTransformation) {
            is FilterTransformation -> {
                FilterToTransformation(loop, inputVariable, targetCollection, previousTransformation.effectiveCondition()) //TODO: use filterNotTo?
            }

            is FlatMapTransformation -> {
                FlatMapToTransformation(loop, previousTransformation.inputVariable, targetCollection, previousTransformation.transform)
            }

            else -> null
        }
    }

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        return KtPsiFactory(loop).createExpressionByPattern("$0 += $1", targetCollection, chainedCallGenerator.receiver)
    }

    /**
     * Matches:
     *     for (...) {
     *         ...
     *         collection.add(...)
     *     }
     */
    object Matcher : ResultTransformationMatcher {
        override fun match(state: MatchingState): ResultTransformationMatch? {
            //TODO: pass indexVariable as null if not used
            if (state.indexVariable != null) return null

            val statement = state.statements.singleOrNull() ?: return null
            //TODO: it can be implicit 'this' too
            val qualifiedExpression = statement as? KtDotQualifiedExpression ?: return null
            val targetCollection = qualifiedExpression.receiverExpression
            //TODO: check that receiver is stable!
            val callExpression = qualifiedExpression.selectorExpression as? KtCallExpression ?: return null
            if (callExpression.getCallNameExpression()?.getReferencedName() != "add") return null
            //TODO: check that it's MutableCollection's add
            val argument = callExpression.valueArguments.singleOrNull() ?: return null
            val argumentValue = argument.getArgumentExpression() ?: return null
            val argumentIsInputVariable = argumentValue.isVariableReference(state.inputVariable)

            //TODO: collection can be used as mutable collection or even ArrayList!
            val collectionInitialization = targetCollection.detectInitializationBeforeLoop(state.outerLoop)
            if (collectionInitialization != null) {
                val collectionKind = collectionInitialization.initializer.isSimpleCollectionInstantiation()
                when (collectionKind) {
                    CollectionKind.LIST -> {
                        val transformation = if (argumentIsInputVariable) {
                            AssignToListTransformation(state.outerLoop, state.inputVariable, collectionInitialization)
                        }
                        else {
                            val mapTransformation = MapTransformation(state.outerLoop, state.inputVariable, argumentValue)
                            AssignSequenceTransformationResultTransformation(mapTransformation, collectionInitialization)
                        }
                        return ResultTransformationMatch(transformation)
                    }

                    CollectionKind.SET -> {
                        if (argumentIsInputVariable) {
                            val transformation = AssignToSetTransformation(state.outerLoop, state.inputVariable, collectionInitialization)
                            return ResultTransformationMatch(transformation)
                        }
                        else {
                            val mapTransformation = MapTransformation(state.outerLoop, state.inputVariable, argumentValue)
                            val transformation = AssignToSetTransformation(
                                    state.outerLoop,
                                    state.inputVariable/*TODO: it's not correct and it looks like not all transformations should have inputVariable*/,
                                    collectionInitialization)
                            return ResultTransformationMatch(transformation, mapTransformation)
                        }
                    }
                }
            }

            val transformation = if (argumentIsInputVariable) {
                AddToCollectionTransformation(state.outerLoop, state.inputVariable, targetCollection)
            }
            else {
                MapToTransformation(state.outerLoop, state.inputVariable, targetCollection, argumentValue)
            }
            return ResultTransformationMatch(transformation)
        }
    }
}

class FilterToTransformation(
        loop: KtForExpression,
        inputVariable: KtCallableDeclaration,
        private val targetCollection: KtExpression,
        private val filter: KtExpression
) : ReplaceLoopResultTransformation(loop, inputVariable) {

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        val lambda = generateLambda(inputVariable, filter)
        return chainedCallGenerator.generate("filterTo($0) $1:'{}'", targetCollection, lambda)
    }
}

class MapToTransformation(
        loop: KtForExpression,
        inputVariable: KtCallableDeclaration,
        private val targetCollection: KtExpression,
        private val mapping: KtExpression
) : ReplaceLoopResultTransformation(loop, inputVariable) {

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        val lambda = generateLambda(inputVariable, mapping)
        return chainedCallGenerator.generate("mapTo($0) $1:'{}'", targetCollection, lambda)
    }
}

class FlatMapToTransformation(
        loop: KtForExpression,
        inputVariable: KtCallableDeclaration,
        private val targetCollection: KtExpression,
        private val transform: KtExpression
) : ReplaceLoopResultTransformation(loop, inputVariable) {

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        val lambda = generateLambda(inputVariable, transform)
        return chainedCallGenerator.generate("flatMapTo($0) $1:'{}'", targetCollection, lambda)
    }
}

class AssignToListTransformation(
        loop: KtForExpression,
        inputVariable: KtCallableDeclaration,
        initialization: VariableInitialization
) : AssignToVariableResultTransformation(loop, inputVariable, initialization) {

    override fun mergeWithPrevious(previousTransformation: SequenceTransformation): ResultTransformation? {
        if (previousTransformation !is FilterTransformation) return null
        return AssignSequenceTransformationResultTransformation(previousTransformation, initialization)
    }

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        return chainedCallGenerator.generate("toList()")
    }
}

class AssignToSetTransformation(
        loop: KtForExpression,
        inputVariable: KtCallableDeclaration,
        initialization: VariableInitialization
) : AssignToVariableResultTransformation(loop, inputVariable, initialization) {

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        return chainedCallGenerator.generate("toSet()")
    }
}
