/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker
import org.jetbrains.kotlin.utils.memoryOptimizedMap

abstract class AbstractIrTypeSubstitutor : TypeSubstitutorMarker {
    abstract fun substitute(type: IrType): IrType
}

abstract class BaseIrTypeSubstitutor(private val irBuiltIns: IrBuiltIns) : AbstractIrTypeSubstitutor() {

    private fun IrType.typeParameterConstructor(): IrTypeParameterSymbol? {
        return if (this is IrSimpleType) classifier as? IrTypeParameterSymbol
        else null
    }

    abstract fun getSubstitutionArgument(typeParameter: IrTypeParameterSymbol): IrTypeArgument

    abstract fun isEmptySubstitution(): Boolean

    final override fun substitute(type: IrType): IrType {
        if (isEmptySubstitution()) return type
        return substituteType(type)
    }

    private fun substituteType(irType: IrType): IrType {
        val substitutedTypeParameter = irType.typeParameterConstructor()?.let {
            when (val typeArgument = getSubstitutionArgument(it)) {
                is IrStarProjection -> irBuiltIns.anyNType // TODO upper bound for T
                is IrTypeProjection -> typeArgument.type.mergeNullability(irType)
            }
        }
        if (substitutedTypeParameter != null) {
            return substitutedTypeParameter
        }

        return when (irType) {
            is IrSimpleType -> with(irType.toBuilder()) {
                arguments = irType.arguments.memoryOptimizedMap { substituteTypeArgument(it) }
                buildSimpleType()
            }
            is IrDynamicType,
            is IrErrorType -> irType
            else -> error("Unexpected type: $irType")
        }
    }

    private fun substituteTypeArgument(typeArgument: IrTypeArgument): IrTypeArgument {
        return when (typeArgument) {
            is IrStarProjection -> typeArgument
            is IrTypeProjection -> makeTypeProjection(substituteType(typeArgument.type), typeArgument.variance)
        }
    }
}

class IrTypeSubstitutor(
    typeParameters: List<IrTypeParameterSymbol>,
    typeArguments: List<IrTypeArgument>,
    irBuiltIns: IrBuiltIns,
    private val allowEmptySubstitution: Boolean = false
) : BaseIrTypeSubstitutor(irBuiltIns) {

    init {
        assert(typeParameters.size == typeArguments.size) {
            "Unexpected number of type arguments: ${typeArguments.size}\n" +
                    "Type parameters are:\n" +
                    typeParameters.joinToString(separator = "\n") { it.owner.render() } +
                    "Type arguments are:\n" +
                    typeArguments.joinToString(separator = "\n") { it.render() }
        }
    }

    private val substitution = typeParameters.zip(typeArguments).toMap()

    override fun getSubstitutionArgument(typeParameter: IrTypeParameterSymbol): IrTypeArgument =
        substitution[typeParameter]
            ?: typeParameter.takeIf { allowEmptySubstitution }?.owner?.defaultType
            ?: error("Unsubstituted type parameter: ${typeParameter.owner.render()}")

    override fun isEmptySubstitution(): Boolean = substitution.isEmpty()
}

class IrCapturedTypeSubstitutor(
    typeParameters: List<IrTypeParameterSymbol>,
    typeArguments: List<IrTypeArgument>,
    capturedTypes: List<IrCapturedType?>,
    irBuiltIns: IrBuiltIns
) : BaseIrTypeSubstitutor(irBuiltIns) {

    init {
        assert(typeArguments.size == typeParameters.size)
        assert(capturedTypes.size == typeParameters.size)
    }

    private val oldSubstitution = typeParameters.zip(typeArguments).toMap()
    private val capturedSubstitution = typeParameters.zip(capturedTypes).toMap()

    override fun getSubstitutionArgument(typeParameter: IrTypeParameterSymbol): IrTypeArgument {
        return capturedSubstitution[typeParameter]?.let { makeTypeProjection(it, Variance.INVARIANT) }
            ?: oldSubstitution[typeParameter]
            ?: throw AssertionError("Unsubstituted type parameter: ${typeParameter.owner.render()}")
    }

    override fun isEmptySubstitution(): Boolean = oldSubstitution.isEmpty()
}

class IrChainedSubstitutor(val first: AbstractIrTypeSubstitutor, val second: AbstractIrTypeSubstitutor) : AbstractIrTypeSubstitutor() {
    override fun substitute(type: IrType): IrType {
        val firstResult = first.substitute(type)
        return second.substitute(firstResult)
    }
}
