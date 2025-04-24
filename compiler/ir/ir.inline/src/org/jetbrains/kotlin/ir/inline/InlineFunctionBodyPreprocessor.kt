/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.utils.memoryOptimizedMap

/**
 * @property typeArguments
 * A map of type parameter symbols to the corresponding types, used for substituting type parameters during inlining.
 * There are 3 cases that can be encountered:
 *   1. The type parameter symbol exists in the map, but the value is null => ERASE the type.
 *   2. The type parameter symbol exists in the map, and there is a non-null value => SUBSTITUTE the type.
 *   3. The type parameter symbol does not exist in the map => LEAVE_AS_IS.
 */
internal class InlineFunctionBodyPreprocessor(
    val typeArguments: Map<IrTypeParameterSymbol, IrType?>,
    val strategy: CallInlinerStrategy,
) {
    @Suppress("UNCHECKED_CAST")
    private val parametersToSubstitute: Map<IrTypeParameterSymbol, IrType> =
        typeArguments.filterValues { it != null } as Map<IrTypeParameterSymbol, IrType>
    private val parametersToErase: Set<IrTypeParameterSymbol> = typeArguments.filter { it.value == null }.keys

    fun preprocess(irElement: IrFunction): IrFunction {
        // Create new symbols.
        irElement.acceptVoid(symbolRemapper)

        // Make symbol remapper aware of the callsite's type arguments.
        symbolRemapper.typeArguments = typeArguments

        // Copy IR.
        val result = irElement.transform(copier, data = null).let {
            // Just a performance optimization. In most cases nothing is to be done in this postprocess, so no reason to traverse the tree.
            if (copier.typeOfNodes.isNotEmpty()) {
                it.transform(TypeOfPostProcessor(), null)
            } else {
                it
            }
        } as IrFunction

        result.patchDeclarationParents(irElement.parent)

        // Make all arguments regular and noinline if needed.
        for ((originalParameter, newParameter) in irElement.parameters.zip(result.parameters)) {
            newParameter.kind = IrParameterKind.Regular
            // It can become inline accidentally because of substitution of type parameter to inline function
            // To revert it we mark it as noinline explicitly
            if (!originalParameter.isInlineParameter() && newParameter.isInlineParameter()) {
                newParameter.isNoinline = true
            }
        }

        return result
    }

    private inner class InlinerTypeRemapper(
        val symbolRemapper: SymbolRemapper,
        val parametersToSubstitute: Map<IrTypeParameterSymbol, IrType>,
        val parametersToErase: Set<IrTypeParameterSymbol>,
    ) : TypeRemapper {

        override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {}

        override fun leaveScope() {}

        private fun remapTypeArguments(
            arguments: List<IrTypeArgument>,
            erasedParameters: MutableSet<IrTypeParameterSymbol>,
        ) =
            arguments.memoryOptimizedMap { argument ->
                when (argument) {
                    is IrStarProjection -> argument
                    is IrTypeProjection -> remapType(argument.type, erasedParameters)
                        ?.let { newType -> makeTypeProjection(newType, argument.variance) }
                        ?: IrStarProjectionImpl
                }
            }

        override fun remapType(type: IrType): IrType {
            return remapType(type, mutableSetOf())
                ?: error("Cannot substitute type ${type.render()}")
        }

        private fun remapType(type: IrType, erasedParameters: MutableSet<IrTypeParameterSymbol>): IrType? {
            if (type !is IrSimpleType) return type

            when (val classifier = type.classifier) {
                in parametersToErase -> {
                    require(classifier is IrTypeParameterSymbol)
                    if (classifier in erasedParameters) {
                        return null
                    }

                    // Pick the (necessarily unique) non-interface upper bound if it exists.
                    val superTypes = classifier.owner.superTypes
                    val superClass = superTypes.firstOrNull {
                        it.classOrNull?.owner?.isInterface == false
                    }

                    val upperBound = superClass ?: superTypes.first()

                    erasedParameters.add(classifier)
                    // TODO: Think about how to reduce complexity from k^N to N^k
                    val erasedUpperBound = remapType(upperBound, erasedParameters)
                        ?: error("Cannot erase upperbound ${upperBound.render()}")
                    erasedParameters.remove(classifier)

                    return erasedUpperBound.mergeNullability(type)
                }
                in parametersToSubstitute -> {
                    val substitutedType = parametersToSubstitute[classifier]!!
                    return when (substitutedType) {
                        is IrDynamicType, is IrErrorType -> substitutedType
                        is IrSimpleType -> substitutedType.mergeNullability(type)
                    }
                }
                // LEAVE_AS_IS
                else -> return type.buildSimpleType {
                    kotlinType = null
                    this.classifier = symbolRemapper.getReferencedClassifier(classifier)
                    arguments = remapTypeArguments(type.arguments, erasedParameters)
                    annotations = type.annotations.memoryOptimizedMap { it.transform(copier, null) as IrConstructorCall }
                }
            }
        }
    }

    private class SymbolRemapperImpl(descriptorsRemapper: DescriptorsRemapper) : DeepCopySymbolRemapper(descriptorsRemapper) {

        var typeArguments: Map<IrTypeParameterSymbol, IrType?>? = null
            set(value) {
                if (field != null) return
                field = value?.asSequence()?.associate {
                    (getReferencedClassifier(it.key) as IrTypeParameterSymbol) to it.value
                }
            }

        override fun getReferencedClassifier(symbol: IrClassifierSymbol): IrClassifierSymbol {
            val result = super.getReferencedClassifier(symbol)
            if (result !is IrTypeParameterSymbol)
                return result
            return typeArguments?.get(result)?.classifierOrNull ?: result
        }
    }

    private val symbolRemapper = SymbolRemapperImpl(NullDescriptorsRemapper)
    private val typeRemapper = InlinerTypeRemapper(symbolRemapper, parametersToSubstitute, parametersToErase)
    private val reifiedTypeSubstitutor = IrTypeSubstitutor(parametersToSubstitute, allowEmptySubstitution = true)
    private val copier = object : DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper) {
        var typeOfNodes = mutableMapOf<IrCall, IrType>()

        override fun visitCall(expression: IrCall): IrCall {
            return super.visitCall(expression).also {
                // We can't do it right now, because we need to return IrCall, and postprocessor for Native
                // want to return IrConstructorCall. In principle this could be done by changing return type in
                // DeepCopyIrTreeWithSymbols, but that's too invasive.
                // So we postpone the postprocessor call for a separate run. This shouldn't be a significant performance hit,
                // as typeOf calls are rare.
                if (Symbols.isTypeOfIntrinsic(expression.symbol)) {
                    typeOfNodes[it] = reifiedTypeSubstitutor.substitute(expression.typeArguments[0]!!)
                }
            }
        }
    }

    inner class TypeOfPostProcessor : IrElementTransformerVoid() {
        override fun visitCall(expression: IrCall): IrExpression {
            expression.transformChildrenVoid(this)
            return copier.typeOfNodes[expression]?.let { oldType ->
                // We should neither erase nor substitute non-reified type parameters in the `typeOf` call so that reflection is able
                // to create a proper KTypeParameter for it. See KT-60175, KT-30279.
                strategy.postProcessTypeOf(expression, oldType)
            } ?: expression
        }
    }
}
