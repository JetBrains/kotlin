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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.*
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.sequence.FilterTransformation
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.sequence.FlatMapTransformation
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.sequence.MapTransformation
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.renderer.render

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

            matchWithCollectionInitializationReplacement(state, targetCollection, argumentValue)
                    ?.let { return it }

            val transformation = if (argumentValue.isVariableReference(state.inputVariable)) {
                AddToCollectionTransformation(state.outerLoop, state.inputVariable, targetCollection)
            }
            else {
                MapToTransformation(state.outerLoop, state.inputVariable, targetCollection, argumentValue)
            }
            return ResultTransformationMatch(transformation)
        }

        private fun matchWithCollectionInitializationReplacement(
                state: MatchingState,
                targetCollection: KtExpression,
                addOperationArgument: KtExpression
        ): ResultTransformationMatch? {
            val collectionInitialization = targetCollection.detectInitializationBeforeLoop(state.outerLoop) ?: return null
            val collectionKind = collectionInitialization.initializer.isSimpleCollectionInstantiation() ?: return null
            val argumentIsInputVariable = addOperationArgument.isVariableReference(state.inputVariable)
            when (collectionKind) {
                CollectionKind.LIST -> {
                    when {
                        canChangeInitializerType(collectionInitialization, KotlinBuiltIns.FQ_NAMES.list, state.outerLoop) -> {
                            val transformation = if (argumentIsInputVariable) {
                                AssignToListTransformation(state.outerLoop, state.inputVariable, collectionInitialization)
                            }
                            else {
                                val mapTransformation = MapTransformation(state.outerLoop, state.inputVariable, addOperationArgument)
                                AssignSequenceTransformationResultTransformation(mapTransformation, collectionInitialization)
                            }
                            return ResultTransformationMatch(transformation)
                        }

                        canChangeInitializerType(collectionInitialization, KotlinBuiltIns.FQ_NAMES.mutableList, state.outerLoop) -> {
                            if (argumentIsInputVariable) {
                                val transformation = AssignToMutableListTransformation(state.outerLoop, state.inputVariable, collectionInitialization)
                                return ResultTransformationMatch(transformation)
                            }
                        }
                    }
                }

                CollectionKind.SET -> {
                    val assignToSetTransformation = when {
                        canChangeInitializerType(collectionInitialization, KotlinBuiltIns.FQ_NAMES.set, state.outerLoop) -> {
                            AssignToSetTransformation(state.outerLoop, state.inputVariable/*TODO: it's not correct and it looks like not all transformations should have inputVariable*/, collectionInitialization)
                        }

                        canChangeInitializerType(collectionInitialization, KotlinBuiltIns.FQ_NAMES.mutableSet, state.outerLoop) -> {
                            AssignToMutableSetTransformation(state.outerLoop, state.inputVariable, collectionInitialization)
                        }

                        else -> return null
                    }

                    if (argumentIsInputVariable) {
                        return ResultTransformationMatch(assignToSetTransformation)
                    }
                    else {
                        val mapTransformation = MapTransformation(state.outerLoop, state.inputVariable, addOperationArgument)
                        return ResultTransformationMatch(assignToSetTransformation, mapTransformation)
                    }
                }
            }

            return null
        }

        private fun canChangeInitializerType(initialization: VariableInitialization, newTypeFqName: FqName, loop: KtForExpression): Boolean {
            val currentType = (initialization.variable.resolveToDescriptor() as VariableDescriptor).type
            if ((currentType.constructor.declarationDescriptor as? ClassDescriptor)?.importableFqName == newTypeFqName) return true // already of the required type

            if (initialization.initializationStatement != initialization.variable) return false

            val newTypeText = newTypeFqName.render() + IdeDescriptorRenderers.SOURCE_CODE.renderTypeArguments(currentType.arguments)
            return canChangeLocalVariableType(initialization.variable, newTypeText, loop)
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

class AssignToMutableListTransformation(
        loop: KtForExpression,
        inputVariable: KtCallableDeclaration,
        initialization: VariableInitialization
) : AssignToVariableResultTransformation(loop, inputVariable, initialization) {

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        return chainedCallGenerator.generate("toMutableList()")
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

class AssignToMutableSetTransformation(
        loop: KtForExpression,
        inputVariable: KtCallableDeclaration,
        initialization: VariableInitialization
) : AssignToVariableResultTransformation(loop, inputVariable, initialization) {

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        return chainedCallGenerator.generate("toMutableSet()")
    }
}
