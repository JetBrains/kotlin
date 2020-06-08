/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.types.KotlinType

interface IrMemberAccessExpression : IrExpression, IrDeclarationReference {
    var dispatchReceiver: IrExpression?
    var extensionReceiver: IrExpression?

    override val symbol: IrSymbol

    val origin: IrStatementOrigin?

    val typeArgumentsCount: Int
    fun getTypeArgument(index: Int): IrType?
    fun putTypeArgument(index: Int, type: IrType?)

    val valueArgumentsCount: Int
    fun getValueArgument(index: Int): IrExpression?
    fun putValueArgument(index: Int, valueArgument: IrExpression?)
    fun removeValueArgument(index: Int)
}

fun IrMemberAccessExpression.getTypeArgument(typeParameterDescriptor: TypeParameterDescriptor): IrType? =
    getTypeArgument(typeParameterDescriptor.index)

fun IrMemberAccessExpression.copyTypeArgumentsFrom(other: IrMemberAccessExpression, shift: Int = 0) {
    assert(typeArgumentsCount == other.typeArgumentsCount + shift) {
        "Mismatching type arguments: $typeArgumentsCount vs ${other.typeArgumentsCount} + $shift"
    }
    for (i in 0 until other.typeArgumentsCount) {
        putTypeArgument(i + shift, other.getTypeArgument(i))
    }
}

inline fun IrMemberAccessExpression.putTypeArguments(
    typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
    toIrType: (KotlinType) -> IrType
) {
    if (typeArguments == null) return
    for ((typeParameter, typeArgument) in typeArguments) {
        putTypeArgument(typeParameter.index, toIrType(typeArgument))
    }
}

val CallableDescriptor.typeParametersCount: Int
    get() =
        when (this) {
            is PropertyAccessorDescriptor -> correspondingProperty.typeParameters.size
            else -> typeParameters.size
        }

fun IrMemberAccessExpression.getTypeArgumentOrDefault(irTypeParameter: IrTypeParameter) =
    getTypeArgument(irTypeParameter.index) ?: irTypeParameter.defaultType

interface IrFunctionAccessExpression : IrMemberAccessExpression {
    override val symbol: IrFunctionSymbol
}

fun IrMemberAccessExpression.getValueArgument(valueParameterDescriptor: ValueParameterDescriptor) =
    getValueArgument(valueParameterDescriptor.index)

fun IrMemberAccessExpression.putValueArgument(valueParameterDescriptor: ValueParameterDescriptor, valueArgument: IrExpression?) {
    putValueArgument(valueParameterDescriptor.index, valueArgument)
}

fun IrMemberAccessExpression.removeValueArgument(valueParameterDescriptor: ValueParameterDescriptor) {
    removeValueArgument(valueParameterDescriptor.index)
}

@DescriptorBasedIr
inline fun <T : IrMemberAccessExpression> T.mapTypeParameters(transform: (TypeParameterDescriptor) -> IrType) : T =
    apply {
        val descriptor = symbol.descriptor as CallableDescriptor
        descriptor.typeParameters.forEach {
            putTypeArgument(it.index, transform(it))
        }
    }

@DescriptorBasedIr
inline fun <T : IrMemberAccessExpression> T.mapValueParameters(transform: (ValueParameterDescriptor) -> IrExpression?): T =
    apply {
        val descriptor = symbol.descriptor as CallableDescriptor
        descriptor.valueParameters.forEach {
            putValueArgument(it.index, transform(it))
        }
    }

@DescriptorBasedIr
inline fun <T : IrMemberAccessExpression> T.mapValueParametersIndexed(transform: (Int, ValueParameterDescriptor) -> IrExpression?): T =
    apply {
        val descriptor = symbol.descriptor as CallableDescriptor
        descriptor.valueParameters.forEach {
            putValueArgument(it.index, transform(it.index, it))
        }
    }

fun IrMemberAccessExpression.putArgument(callee: IrFunction, parameter: IrValueParameter, argument: IrExpression) =
    when (parameter) {
        callee.dispatchReceiverParameter -> dispatchReceiver = argument
        callee.extensionReceiverParameter -> extensionReceiver = argument
        else -> putValueArgument(parameter.index, argument)
    }

fun IrFunctionAccessExpression.putArgument(parameter: IrValueParameter, argument: IrExpression) =
    putArgument(symbol.owner, parameter, argument)
