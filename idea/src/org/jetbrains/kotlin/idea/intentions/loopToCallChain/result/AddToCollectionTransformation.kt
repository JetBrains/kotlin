/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions.loopToCallChain.result

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.*
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.sequence.*
import org.jetbrains.kotlin.idea.references.resolveMainReferenceToDescriptors
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeUniqueAsSequence

class AddToCollectionTransformation(
    loop: KtForExpression,
    private val targetCollection: KtExpression
) : ReplaceLoopResultTransformation(loop) {

    override fun mergeWithPrevious(previousTransformation: SequenceTransformation, reformat: Boolean): ResultTransformation? =
        when (previousTransformation) {
            is FilterTransformation -> FilterToTransformation.create(
                loop, previousTransformation.inputVariable, previousTransformation.indexVariable,
                targetCollection, previousTransformation.effectiveCondition, previousTransformation.isFilterNot
            )

            is FilterNotNullTransformation -> FilterNotNullToTransformation.create(loop, targetCollection)

            is MapTransformation -> MapToTransformation.create(
                loop,
                previousTransformation.inputVariable,
                previousTransformation.indexVariable,
                targetCollection,
                previousTransformation.mapping,
                previousTransformation.mapNotNull
            )

            is FlatMapTransformation -> FlatMapToTransformation.create(
                loop,
                previousTransformation.inputVariable,
                targetCollection,
                previousTransformation.transform
            )

            else -> null
        }

    override val presentation: String
        get() = "+="

    override fun buildPresentation(prevTransformationsPresentation: String?): String {
        return if (prevTransformationsPresentation != null)
            "+= $prevTransformationsPresentation"
        else
            "+="
    }

    override val chainCallCount: Int
        get() = 0

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        return KtPsiFactory(loop).createExpressionByPattern(
            "$0 += $1", targetCollection, chainedCallGenerator.receiver,
            reformat = chainedCallGenerator.reformat
        )
    }

    /**
     * Matches:
     *     for (...) {
     *         ...
     *         collection.add(...)
     *     }
     */
    object Matcher : TransformationMatcher {
        override val indexVariableAllowed: Boolean
            get() = true

        override fun match(state: MatchingState): TransformationMatch.Result? {
            val statement = state.statements.singleOrNull() ?: return null
            //TODO: it can be implicit 'this' too
            val qualifiedExpression = statement as? KtDotQualifiedExpression ?: return null
            val targetCollection = qualifiedExpression.receiverExpression
            val callExpression = qualifiedExpression.selectorExpression as? KtCallExpression ?: return null
            val callNameExpression = callExpression.getCallNameExpression() ?: return null
            if (callNameExpression.getReferencedName() != "add") return null
            val calleeMethodDescriptors = callNameExpression.resolveMainReferenceToDescriptors()
            if (calleeMethodDescriptors.none { it.isCollectionAdd() }) return null

            val argument = callExpression.valueArguments.singleOrNull() ?: return null
            val argumentValue = argument.getArgumentExpression() ?: return null

            //TODO: should work when ".asSequence()" used when there are other read-usages in the loop
            if (!targetCollection.isStableInLoop(state.outerLoop, checkNoOtherUsagesInLoop = true)) return null

            if (state.indexVariable == null) {
                matchWithCollectionInitializationReplacement(state, targetCollection, argumentValue)
                    ?.let { return it }
            }

            return if (state.indexVariable == null && argumentValue.isVariableReference(state.inputVariable)) {
                TransformationMatch.Result(AddToCollectionTransformation(state.outerLoop, targetCollection))
            } else {
                if (state.outerLoop.loopRange.isRangeLiteral())
                    null
                else {
                    //TODO: recognize "?: continue" in the argument
                    TransformationMatch.Result(
                        MapToTransformation.create(
                            state.outerLoop, state.inputVariable, state.indexVariable, targetCollection, argumentValue, mapNotNull = false
                        )
                    )
                }
            }
        }

        private fun matchWithCollectionInitializationReplacement(
            state: MatchingState,
            targetCollection: KtExpression,
            addOperationArgument: KtExpression
        ): TransformationMatch.Result? {
            val collectionInitialization =
                targetCollection.findVariableInitializationBeforeLoop(state.outerLoop, checkNoOtherUsagesInLoop = true) ?: return null
            val collectionKind = collectionInitialization.initializer.isSimpleCollectionInstantiation() ?: return null
            val argumentIsInputVariable = addOperationArgument.isVariableReference(state.inputVariable)
            when (collectionKind) {
                CollectionKind.LIST -> {
                    when {
                        canChangeInitializerType(collectionInitialization, StandardNames.FqNames.list, state.outerLoop) -> {
                            return if (argumentIsInputVariable) {
                                val assignToList = AssignToListTransformation(state.outerLoop, collectionInitialization, state.lazySequence)
                                TransformationMatch.Result(assignToList)
                            } else {
                                val mapTransformation =
                                    MapTransformation(state.outerLoop, state.inputVariable, null, addOperationArgument, mapNotNull = false)
                                if (state.lazySequence) {
                                    val assignToList =
                                        AssignToListTransformation(state.outerLoop, collectionInitialization, lazySequence = true)
                                    TransformationMatch.Result(assignToList, mapTransformation)
                                } else {
                                    val assignSequence = AssignSequenceResultTransformation(mapTransformation, collectionInitialization)
                                    TransformationMatch.Result(assignSequence)
                                }
                            }
                        }

                        canChangeInitializerType(collectionInitialization, StandardNames.FqNames.mutableList, state.outerLoop) -> {
                            if (argumentIsInputVariable) {
                                val transformation = AssignToMutableListTransformation(state.outerLoop, collectionInitialization)
                                return TransformationMatch.Result(transformation)
                            }
                        }
                    }
                }

                CollectionKind.SET -> {
                    val assignToSetTransformation = when {
                        canChangeInitializerType(collectionInitialization, StandardNames.FqNames.set, state.outerLoop) -> {
                            AssignToSetTransformation(state.outerLoop, collectionInitialization)
                        }

                        canChangeInitializerType(collectionInitialization, StandardNames.FqNames.mutableSet, state.outerLoop) -> {
                            AssignToMutableSetTransformation(state.outerLoop, collectionInitialization)
                        }

                        else -> return null
                    }

                    return if (argumentIsInputVariable) {
                        TransformationMatch.Result(assignToSetTransformation)
                    } else {
                        val mapTransformation =
                            MapTransformation(state.outerLoop, state.inputVariable, null, addOperationArgument, mapNotNull = false)
                        TransformationMatch.Result(assignToSetTransformation, mapTransformation)
                    }
                }
            }

            return null
        }

        private fun canChangeInitializerType(
            initialization: VariableInitialization,
            newTypeFqName: FqName,
            loop: KtForExpression
        ): Boolean {
            val currentType = (initialization.variable.unsafeResolveToDescriptor() as VariableDescriptor).type
            // already of the required type
            if ((currentType.constructor.declarationDescriptor as? ClassDescriptor)?.importableFqName == newTypeFqName) return true

            // we do not change explicit type
            if (initialization.variable.typeReference != null) return false

            if (initialization.initializationStatement != initialization.variable) return false

            val newTypeText = newTypeFqName.render() + IdeDescriptorRenderers.SOURCE_CODE.renderTypeArguments(currentType.arguments)
            return canChangeLocalVariableType(initialization.variable, newTypeText, loop)
        }
    }
}

private fun KtExpression?.isRangeLiteral() = this is KtBinaryExpression && operationToken == KtTokens.RANGE

private fun DeclarationDescriptor.isCollectionAdd(): Boolean {
    if ((containingDeclaration as? ClassDescriptor)?.fqNameSafe == StandardNames.FqNames.mutableCollection) {
        return true
    }

    val overrides = (this as? CallableDescriptor)?.overriddenTreeUniqueAsSequence(true) ?: return false
    return overrides.any {
        (it.containingDeclaration as? ClassDescriptor)?.fqNameSafe == StandardNames.FqNames.mutableCollection
    }
}

class FilterToTransformation private constructor(
    loop: KtForExpression,
    private val inputVariable: KtCallableDeclaration,
    private val indexVariable: KtCallableDeclaration?,
    private val targetCollection: KtExpression,
    private val effectiveCondition: Condition,
    private val isFilterNot: Boolean
) : ReplaceLoopResultTransformation(loop) {

    init {
        if (isFilterNot) {
            assert(indexVariable == null)
        }
    }

    private val functionName = when {
        indexVariable != null -> "filterIndexedTo"
        isFilterNot -> "filterNotTo"
        else -> "filterTo"
    }

    override val presentation: String
        get() = "$functionName(){}"

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        val reformat = chainedCallGenerator.reformat
        val lambda = if (indexVariable != null)
            generateLambda(inputVariable, indexVariable, effectiveCondition.asExpression(reformat), reformat)
        else
            generateLambda(
                inputVariable,
                if (isFilterNot) effectiveCondition.asNegatedExpression(reformat) else effectiveCondition.asExpression(reformat),
                reformat
            )
        return chainedCallGenerator.generate("$functionName($0) $1:'{}'", targetCollection, lambda)
    }

    companion object {
        fun create(
            loop: KtForExpression,
            inputVariable: KtCallableDeclaration,
            indexVariable: KtCallableDeclaration?,
            targetCollection: KtExpression,
            condition: Condition,
            isFilterNot: Boolean
        ): ResultTransformation {
            val initialization = targetCollection.findVariableInitializationBeforeLoop(loop, checkNoOtherUsagesInLoop = true)
            return if (initialization != null && initialization.initializer.hasNoSideEffect()) {
                val transformation =
                    FilterToTransformation(loop, inputVariable, indexVariable, initialization.initializer, condition, isFilterNot)
                AssignToVariableResultTransformation.createDelegated(transformation, initialization)
            } else {
                FilterToTransformation(loop, inputVariable, indexVariable, targetCollection, condition, isFilterNot)
            }
        }
    }
}

class FilterNotNullToTransformation private constructor(
    loop: KtForExpression,
    private val targetCollection: KtExpression
) : ReplaceLoopResultTransformation(loop) {

    override val presentation: String
        get() = "filterNotNullTo()"

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        return chainedCallGenerator.generate("filterNotNullTo($0)", targetCollection)
    }

    companion object {
        fun create(
            loop: KtForExpression,
            targetCollection: KtExpression
        ): ResultTransformation {
            val initialization = targetCollection.findVariableInitializationBeforeLoop(loop, checkNoOtherUsagesInLoop = true)
            return if (initialization != null && initialization.initializer.hasNoSideEffect()) {
                val transformation = FilterNotNullToTransformation(loop, initialization.initializer)
                AssignToVariableResultTransformation.createDelegated(transformation, initialization)
            } else {
                FilterNotNullToTransformation(loop, targetCollection)
            }
        }
    }
}

class MapToTransformation private constructor(
    loop: KtForExpression,
    private val inputVariable: KtCallableDeclaration,
    private val indexVariable: KtCallableDeclaration?,
    private val targetCollection: KtExpression,
    private val mapping: KtExpression,
    mapNotNull: Boolean
) : ReplaceLoopResultTransformation(loop) {

    private val functionName = if (indexVariable != null)
        if (mapNotNull) "mapIndexedNotNullTo" else "mapIndexedTo"
    else
        if (mapNotNull) "mapNotNullTo" else "mapTo"

    override val presentation: String
        get() = "$functionName(){}"

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        val lambda = generateLambda(inputVariable, indexVariable, mapping, chainedCallGenerator.reformat)
        return chainedCallGenerator.generate("$functionName($0) $1:'{}'", targetCollection, lambda)
    }

    companion object {
        fun create(
            loop: KtForExpression,
            inputVariable: KtCallableDeclaration,
            indexVariable: KtCallableDeclaration?,
            targetCollection: KtExpression,
            mapping: KtExpression,
            mapNotNull: Boolean
        ): ResultTransformation {
            val initialization = targetCollection.findVariableInitializationBeforeLoop(loop, checkNoOtherUsagesInLoop = true)
            return if (initialization != null && initialization.initializer.hasNoSideEffect()) {
                val transformation =
                    MapToTransformation(loop, inputVariable, indexVariable, initialization.initializer, mapping, mapNotNull)
                AssignToVariableResultTransformation.createDelegated(transformation, initialization)
            } else {
                MapToTransformation(loop, inputVariable, indexVariable, targetCollection, mapping, mapNotNull)
            }
        }
    }
}

class FlatMapToTransformation private constructor(
    loop: KtForExpression,
    private val inputVariable: KtCallableDeclaration,
    private val targetCollection: KtExpression,
    private val transform: KtExpression
) : ReplaceLoopResultTransformation(loop) {

    override val presentation: String
        get() = "flatMapTo(){}"

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        val lambda = generateLambda(inputVariable, transform, chainedCallGenerator.reformat)
        return chainedCallGenerator.generate("flatMapTo($0) $1:'{}'", targetCollection, lambda)
    }

    // asSequence().flatMapTo(){} makes real difference (because expression inside lambda is evaluated lazily)
    //TODO: return false if expression is input variable
    override val lazyMakesSense: Boolean
        get() = true

    companion object {
        fun create(
            loop: KtForExpression,
            inputVariable: KtCallableDeclaration,
            targetCollection: KtExpression,
            transform: KtExpression
        ): ResultTransformation {
            val initialization = targetCollection.findVariableInitializationBeforeLoop(loop, checkNoOtherUsagesInLoop = true)
            return if (initialization != null && initialization.initializer.hasNoSideEffect()) {
                val transformation = FlatMapToTransformation(loop, inputVariable, initialization.initializer, transform)
                AssignToVariableResultTransformation.createDelegated(transformation, initialization)
            } else {
                FlatMapToTransformation(loop, inputVariable, targetCollection, transform)
            }
        }
    }
}

class AssignToListTransformation(
    loop: KtForExpression,
    initialization: VariableInitialization,
    private val lazySequence: Boolean
) : AssignToVariableResultTransformation(loop, initialization) {

    override val presentation: String
        get() = "toList()"

    override fun mergeWithPrevious(previousTransformation: SequenceTransformation, reformat: Boolean): ResultTransformation? {
        if (lazySequence) return null // toList() is necessary if the result is Sequence
        //TODO: can be any SequenceTransformation's that return not List<T>?
        return AssignSequenceResultTransformation(previousTransformation, initialization)
    }

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        return chainedCallGenerator.generate("toList()")
    }
}

class AssignToMutableListTransformation(
    loop: KtForExpression,
    initialization: VariableInitialization
) : AssignToVariableResultTransformation(loop, initialization) {

    override val presentation: String
        get() = "toMutableList()"

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        return chainedCallGenerator.generate("toMutableList()")
    }
}

class AssignToSetTransformation(
    loop: KtForExpression,
    initialization: VariableInitialization
) : AssignToVariableResultTransformation(loop, initialization) {

    override val presentation: String
        get() = "toSet()"

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        return chainedCallGenerator.generate("toSet()")
    }
}

class AssignToMutableSetTransformation(
    loop: KtForExpression,
    initialization: VariableInitialization
) : AssignToVariableResultTransformation(loop, initialization) {

    override val presentation: String
        get() = "toMutableSet()"

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        return chainedCallGenerator.generate("toMutableSet()")
    }
}
