/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrMemberAccessExpressionBase
import org.jetbrains.kotlin.types.KotlinType

fun remapTypeArguments(
    expression: IrMemberAccessExpression,
    newDescriptor: CallableDescriptor
): Map<TypeParameterDescriptor, KotlinType>? {
    val oldDescriptor = expression.descriptor
    val oldTypeArguments = expression.getTypeArgumentsMap()
    val oldTypeParameters = oldDescriptor.original.typeParameters
    val newTypeParameters = newDescriptor.original.typeParameters

    assert(oldTypeParameters.size == newTypeParameters.size) {
        "Mismatching type parameters: oldDescriptor: $oldDescriptor; newDescriptor: $newDescriptor"
    }

    return when {
        oldDescriptor.original == newDescriptor.original -> oldTypeArguments

        oldTypeArguments == null || oldTypeArguments.isEmpty() -> null

        else -> newTypeParameters.associate { newTypeParameter ->
            val oldTypeParameter = oldTypeParameters[newTypeParameter.index]
            val newTypeArgument = expression.getTypeArgument(oldTypeParameter)
                    ?: throw AssertionError("No type argument for $newTypeParameter <= $oldTypeParameter")
            newTypeParameter to newTypeArgument
        }
    }
}

fun IrMemberAccessExpression.getTypeArgumentsMap(): Map<TypeParameterDescriptor, KotlinType>? {
    if (this is IrMemberAccessExpressionBase) return typeArguments

    val typeParameters = descriptor.original.typeParameters
    return if (typeParameters.isEmpty())
        null
    else
        typeParameters.associateBy({ it }, { getTypeArgument(it)!! })
}