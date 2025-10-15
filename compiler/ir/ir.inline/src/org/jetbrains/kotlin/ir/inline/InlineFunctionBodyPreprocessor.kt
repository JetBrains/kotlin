/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.ir.PreSerializationSymbols
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid

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
) {
    @Suppress("UNCHECKED_CAST")
    private val parametersToSubstitute: Map<IrTypeParameterSymbol, IrType> =
        typeArguments.filterValues { it != null } as Map<IrTypeParameterSymbol, IrType>
    private val parametersToErase: Set<IrTypeParameterSymbol> = typeArguments.filter { it.value == null }.keys

    fun preprocess(irElement: IrFunction): IrFunction {
        // Create new symbols.
        irElement.acceptVoid(symbolRemapper)

        // Copy IR.
        val result = irElement.transform(copier, data = null) as IrFunction

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

    private val symbolRemapper = object : DeepCopySymbolRemapper() {
        override fun getReferencedTypeParameter(symbol: IrTypeParameterSymbol): IrClassifierSymbol {
            // erased symbols can't come here, except for a single case - inside typeOf call type argument.
            // in all other cases they would be substituted before coming here. For typeOf we need to just keep them as is,
            // so let's do it.
            if (symbol in parametersToErase) return symbol
            return super.getReferencedTypeParameter(symbol)
        }
    }
    private val nonReifiedTypeParameterSubstitutor: AbstractIrTypeSubstitutor = object : BaseIrTypeSubstitutor() {
        private val inProgress = mutableSetOf<IrTypeParameterSymbol>()
        // We need to avoid computing erasures for unused parameters, as they still exist in functions
        // loaded from klibs, but computing erasure would lead to exception because of unbound symbols
        // in super-types. So we cache them on-demand instead of computing in advance.
        private val erasureCache = mutableMapOf<IrTypeParameterSymbol, IrType>()
        override fun getSubstitutionArgument(typeParameter: IrTypeParameterSymbol): IrTypeArgument {
            if (typeParameter !in parametersToErase) return typeParameter.defaultType
            // We have found a type with recursive upper bound. Let's erase it to star at some point.
            // That's not correct, but the best we can do.
            if (typeParameter in inProgress) return IrStarProjectionImpl
            return erasureCache.getOrPut(typeParameter) {
                // Pick the (necessarily unique) non-interface upper bound if it exists.
                val superTypes = typeParameter.owner.superTypes
                val superClass = superTypes.firstOrNull {
                    it.classOrNull?.owner?.isInterface == false
                }
                val upperBound = superClass ?: superTypes.first()
                inProgress.add(typeParameter)
                // Note: an upper bound can be another type parameter, and it can be reified.
                // So we need to call [allTypeParameterSubstitutor], not just substitute recursively by ourselves.
                // As this remapper is chained, it can call this remapper back if needed.
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

    /**
     * There are several types of classifiers that can happen inside the tree:
     * * Classes defined outside the inline function (should be kept as is, can be unbound)
     * * Local classes defined inside the inline function (need to be replaced with copied local class symbol)
     * * Type parameters that need to be substituted (reified type parameters)
     * * Type parameters that need to be erased (non-reifed type parameters and some type parameters of outer scopes)
     * * Type parameters that need to be kept as is (type parameters of common outer scopes of inline function and its call-site)
     * * Type parameters that need to be remapped to type parameter of copied function (reified in case of storing to klib)
     *
     * Also, there is a special case -- in typeOf function type argument position type parameters that are not substituted need to be kept as is.
     *
     * To achieve this, we have several type substitutors and use appropriate ones in different places.
     *
     * 1. [nonReifiedTypeParameterSubstitutor] substitutes type parameters with their erasure, when it's required
     * 2. [reifiedTypeParameterSubstitutor] substitutes type parameters with their call-site value when it's required.
     * 3. [allTypeParameterSubstitutor] do both 1 and 2 at the same time.
     * 4. Type remapper within [DeepCopyIrTreeWithSymbols] remaps all kinds of local symbols (both type parameters and local classes)
     *    * [symbolRemapper] is it's part, and can be used separately if needed to do this substitution on classifiers.
     *    * As a special case [symbolRemapper] keeps references to erased symbols in place. See comments inside it for details.
     */
    private val allTypeParameterSubstitutor = reifiedTypeParameterSubstitutor.chainedWith(nonReifiedTypeParameterSubstitutor)

    private val copier = object : DeepCopyIrTreeWithSymbols(symbolRemapper, null) {

        override fun remapTypeImpl(type: IrType): IrType {
            return super.remapTypeImpl(allTypeParameterSubstitutor.substitute(type))
        }

        override fun visitCall(expression: IrCall): IrCall {
            return super.visitCall(expression).also {
                if (PreSerializationSymbols.isTypeOfIntrinsic(expression.symbol)) {
                    // We need to call super.remap here because we need to remap local classes.
                    it.typeArguments[0] = super.remapTypeImpl(reifiedTypeParameterSubstitutor.substitute(expression.typeArguments[0]!!))
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

}
