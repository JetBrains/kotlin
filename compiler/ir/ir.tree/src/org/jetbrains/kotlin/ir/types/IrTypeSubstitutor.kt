/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.types.TypeSubstitutor


class IrTypeSubstitutor(
    typeParameters: List<IrTypeParameterSymbol>,
    typeArguments: List<IrTypeArgument>,
    private val irBuiltIns: IrBuiltIns
) {
    private val substitution = typeParameters.zip(typeArguments).toMap()

    private fun IrType.typeParameterConstructor(): IrTypeParameterSymbol? {
        return if (this is IrSimpleType) classifier as? IrTypeParameterSymbol
        else null
    }

    fun substitute(type: IrType): IrType {
        if (substitution.isEmpty()) return type

        return type.typeParameterConstructor()?.let {
            // check whether it's T or T?
            val isNullable = type.isMarkedNullable()
            val typeArgument = substitution.getValue(it)
            when (typeArgument) {
                is IrStarProjection -> if (isNullable) irBuiltIns.anyNType else irBuiltIns.anyType
                is IrTypeProjection -> with(typeArgument.type) { if (isNullable) makeNullable() else makeNotNull() }
                else -> error("unknown type argument")
            }
        } ?: substituteType(type)
    }

    private fun substituteType(irType: IrType): IrType {
        return when (irType) {
            is IrDynamicType -> irType
            is IrErrorType -> irType
            else -> {
                require(irType is IrSimpleType)
                with(irType.toBuilder()) {
                    arguments = irType.arguments.map { substituteTypeArgument(it) }
                    buildSimpleType()
                }
            }
        }
    }

    private fun substituteTypeArgument(typeArgument: IrTypeArgument): IrTypeArgument {
        if (typeArgument is IrStarProjection) return typeArgument

        require(typeArgument is IrTypeProjection)

        val type = typeArgument.type
        if (type is IrSimpleType) {
            val classifier = type.classifier
            if (classifier is IrTypeParameterSymbol) {
                val newArgument = substitution.getValue(classifier)
                return if (newArgument is IrTypeProjection) {
                    makeTypeProjection(newArgument.type, typeArgument.variance)
                } else newArgument
            }
        }
        return makeTypeProjection(substituteType(typeArgument.type), typeArgument.variance)
    }
}
