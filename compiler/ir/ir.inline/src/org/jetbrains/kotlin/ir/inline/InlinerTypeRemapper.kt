/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.utils.memoryOptimizedMap

internal class InlinerTypeRemapper(
    private val symbolRemapper: SymbolRemapper,
    private val typeArguments: Map<IrTypeParameterSymbol, IrType?>?,
) : TypeRemapper {
    lateinit var copier: DeepCopyIrTreeWithSymbols

    override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {}

    override fun leaveScope() {}

    private fun remapTypeArguments(
        arguments: List<IrTypeArgument>,
        erasedParameters: MutableSet<IrTypeParameterSymbol>?,
        leaveNonReifiedAsIs: Boolean,
    ) =
        arguments.memoryOptimizedMap { argument ->
            (argument as? IrTypeProjection)?.let { proj ->
                remapType(proj.type, erasedParameters, leaveNonReifiedAsIs)?.let { newType ->
                    makeTypeProjection(newType, proj.variance)
                } ?: IrStarProjectionImpl
            }
                ?: argument
        }

    override fun remapType(type: IrType) = remapType(type, NonReifiedTypeParameterRemappingMode.ERASE)

    fun remapType(type: IrType, mode: NonReifiedTypeParameterRemappingMode): IrType {
        val erasedParams = if (mode == NonReifiedTypeParameterRemappingMode.ERASE) mutableSetOf<IrTypeParameterSymbol>() else null
        return remapType(type, erasedParams, mode == NonReifiedTypeParameterRemappingMode.LEAVE_AS_IS)
            ?: error("Cannot substitute type ${type.render()}")
    }

    private fun remapType(type: IrType, erasedParameters: MutableSet<IrTypeParameterSymbol>?, leaveNonReifiedAsIs: Boolean): IrType? {
        if (type !is IrSimpleType) return type

        val classifier = type.classifier
        val substitutedType = typeArguments?.get(classifier)

        if (leaveNonReifiedAsIs && classifier is IrTypeParameterSymbol && !classifier.owner.isReified) {
            return type
        }

        // Erase non-reified type parameter if asked to.
        if (erasedParameters != null && substitutedType != null && (classifier as? IrTypeParameterSymbol)?.owner?.isReified == false) {

            if (classifier in erasedParameters) {
                return null
            }

            erasedParameters.add(classifier)

            // Pick the (necessarily unique) non-interface upper bound if it exists.
            val superTypes = classifier.owner.superTypes
            val superClass = superTypes.firstOrNull {
                it.classOrNull?.owner?.isInterface == false
            }

            val upperBound = superClass ?: superTypes.first()

            // TODO: Think about how to reduce complexity from k^N to N^k
            val erasedUpperBound = remapType(upperBound, erasedParameters, leaveNonReifiedAsIs)
                ?: error("Cannot erase upperbound ${upperBound.render()}")

            erasedParameters.remove(classifier)

            return erasedUpperBound.mergeNullability(type)
        }

        if (substitutedType is IrDynamicType) return substitutedType

        if (substitutedType is IrSimpleType) {
            return substitutedType.mergeNullability(type)
        }

        return type.buildSimpleType {
            kotlinType = null
            this.classifier = symbolRemapper.getReferencedClassifier(classifier)
            arguments = remapTypeArguments(type.arguments, erasedParameters, leaveNonReifiedAsIs)
            annotations = type.annotations.memoryOptimizedMap { it.transform(copier, null) as IrConstructorCall }
        }
    }
}