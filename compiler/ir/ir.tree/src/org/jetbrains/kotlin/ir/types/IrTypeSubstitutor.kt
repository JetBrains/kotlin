/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.impl.originalKotlinType
import org.jetbrains.kotlin.types.Variance


class IrTypeSubstitutor(
    typeParameters: List<IrTypeParameterSymbol>,
    typeArguments: List<IrTypeArgument>,
    private val irBuiltIns: IrBuiltIns
) {
    private val substitution = typeParameters.zip(typeArguments).toMap()

    fun substitute(type: IrType) = if (substitution.isNotEmpty()) doSubstituteType(type) else type

    private fun doSubstituteTypeArgument(typeArgument: IrTypeArgument): IrTypeArgument {
        if (typeArgument is IrStarProjection) return typeArgument

        require(typeArgument is IrTypeProjection)

        return if (typeArgument is IrSimpleType) {
            val classifier = typeArgument.classifier
            if (classifier is IrTypeParameterSymbol) substitution.getValue(classifier)
            else {
                makeTypeProjection(doSubstituteType(typeArgument), Variance.INVARIANT)
            }
        } else makeTypeProjection(doSubstituteType(typeArgument.type), typeArgument.variance)
    }

    private fun doSubstituteType(type: IrType): IrType = when (type) {
        is IrErrorType -> type
        is IrDynamicType -> type
        is IrSimpleType -> {
            val classifier = type.classifier
            if (classifier is IrTypeParameterSymbol) {
                val argument = substitution.getValue(classifier)
                if (argument is IrTypeProjection) {
                    argument.type
                } else irBuiltIns.anyNType // StarProjection
            } else {
                type.run {
                    IrSimpleTypeImpl(
                        originalKotlinType,
                        classifier,
                        hasQuestionMark,
                        arguments.map { doSubstituteTypeArgument(it) },
                        annotations
                    )
                }
            }
        }
        else -> error("Unknown type")
    }
}
