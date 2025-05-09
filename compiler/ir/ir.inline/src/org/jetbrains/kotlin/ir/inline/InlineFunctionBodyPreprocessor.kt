/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

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

    private val symbolRemapper = DeepCopySymbolRemapper(NullDescriptorsRemapper)

    // We need to avoid computing erasures for unused parameters, as they still exist in functions
    // loaded from klibs, but computing erasure would lead to exception because of unbound symbols
    // in super-types.
    private val nonReifiedTypeParameterSubstitutor: AbstractIrTypeSubstitutor = object : BaseIrTypeSubstitutor() {
        private val inProgress = mutableSetOf<IrTypeParameterSymbol>()
        private val erasureCache = mutableMapOf<IrTypeParameterSymbol, IrType>()
        override fun getSubstitutionArgument(typeParameter: IrTypeParameterSymbol): IrTypeArgument {
            if (typeParameter !in parametersToErase) return typeParameter.defaultType
            // We have found a type with recursive upper bound. Let's erase it to star at some point.
            if (typeParameter in inProgress) return IrStarProjectionImpl
            return erasureCache.getOrPut(typeParameter) {
                // Pick the (necessarily unique) non-interface upper bound if it exists.
                val superTypes = typeParameter.owner.superTypes
                val superClass = superTypes.firstOrNull {
                    it.classOrNull?.owner?.isInterface == false
                }
                val upperBound = superClass ?: superTypes.first()
                inProgress.add(typeParameter)
                val substitutedUpperBound = allTypeParameterSubstitutor.substitute(upperBound)
                inProgress.remove(typeParameter)
                substitutedUpperBound
            }
        }

        override fun isEmptySubstitution(): Boolean {
            return parametersToErase.isEmpty()
        }
    }
    private val reifiedTypeParameterSubstitutor = IrTypeSubstitutor(parametersToSubstitute, allowEmptySubstitution = true)
    private val allTypeParameterSubstitutor = reifiedTypeParameterSubstitutor.chainedWith(nonReifiedTypeParameterSubstitutor)

    private val copier = object : DeepCopyIrTreeWithSymbols(symbolRemapper, null) {
        var typeOfNodes = mutableMapOf<IrCall, IrType>()

        override fun remapTypeImpl(type: IrType): IrType {
            return super.remapTypeImpl(allTypeParameterSubstitutor.substitute(type))
        }

        override fun visitCall(expression: IrCall): IrCall {
            return super.visitCall(expression).also {
                // We can't do it right now, because we need to return IrCall, and postprocessor for Native
                // want to return IrConstructorCall. In principle this could be done by changing return type in
                // DeepCopyIrTreeWithSymbols, but that's too invasive.
                // So we postpone the postprocessor call for a separate run. This shouldn't be a significant performance hit,
                // as typeOf calls are rare.
                if (Symbols.isTypeOfIntrinsic(expression.symbol)) {
                    // We need to call super.remap here because we need to remap local classes.
                    typeOfNodes[it] = super.remapTypeImpl(reifiedTypeParameterSubstitutor.substitute(expression.typeArguments[0]!!))
                }
            }
        }

        override fun visitClassReference(expression: IrClassReference): IrClassReference {
            return super.visitClassReference(expression).also {
                val symbol = expression.symbol
                if (symbol is IrTypeParameterSymbol) {
                    val replacement = reifiedTypeParameterSubstitutor.getSubstitutionArgument(symbol)?.typeOrFail?.classifierOrFail ?: symbol
                    it.symbol = symbolRemapper.getReferencedClassifier(replacement)
                }
            }
        }
    }

    inner class TypeOfPostProcessor : IrElementTransformerVoid() {
        // Non-reified type parameters were substituted by corresponding parameters of the copied function
        // That's not correct, we need to return them back for typeOf calls.
        private val nonReifiedTypeParameterUnsubsitutor = IrTypeSubstitutor(
            parametersToErase.associate { (symbolRemapper.getReferencedTypeParameter(it) as IrTypeParameterSymbol) to it.defaultType },
            allowEmptySubstitution = true
        )

        override fun visitCall(expression: IrCall): IrExpression {
            expression.transformChildrenVoid(this)
            return copier.typeOfNodes[expression]?.let { oldType ->
                // We should neither erase nor substitute non-reified type parameters in the `typeOf` call so that reflection is able
                // to create a proper KTypeParameter for it. See KT-60175, KT-30279.
                strategy.postProcessTypeOf(expression, nonReifiedTypeParameterUnsubsitutor.substitute(oldType))
            } ?: expression
        }
    }
}
