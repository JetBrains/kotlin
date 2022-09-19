/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType

// todo: autogenerate
/**
 * A non-leaf IR tree element.
 */
abstract class IrMemberAccessExpression<S : IrSymbol> : IrDeclarationReference() {
    var dispatchReceiver: IrExpression? = null
    var extensionReceiver: IrExpression? = null

    abstract override val symbol: S

    abstract val origin: IrStatementOrigin?

    protected abstract val typeArgumentsByIndex: Array<IrType?>
    val typeArgumentsCount: Int get() = typeArgumentsByIndex.size

    protected abstract val argumentsByParameterIndex: Array<IrExpression?>
    open val valueArgumentsCount: Int get() = argumentsByParameterIndex.size

    fun getValueArgument(index: Int): IrExpression? {
        if (index >= valueArgumentsCount) {
            throw AssertionError("$this: No such value argument slot: $index")
        }
        if (index == -1)
            Unit
        return argumentsByParameterIndex[index]
    }

    fun putValueArgument(index: Int, valueArgument: IrExpression?) {
        if (index >= valueArgumentsCount) {
            throw AssertionError("$this: No such value argument slot: $index")
        }
        argumentsByParameterIndex[index] = valueArgument
    }

    fun getTypeArgument(index: Int): IrType? {
        if (index >= typeArgumentsCount) {
            throwNoSuchArgumentSlotException("type", index, typeArgumentsCount)
        }
        return typeArgumentsByIndex[index]
    }

    fun putTypeArgument(index: Int, type: IrType?) {
        if (index >= typeArgumentsCount) {
            throwNoSuchArgumentSlotException("type", index, typeArgumentsCount)
        }
        typeArgumentsByIndex[index] = type
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        dispatchReceiver?.accept(visitor, data)
        extensionReceiver?.accept(visitor, data)
        if (valueArgumentsCount > 0) {
            argumentsByParameterIndex.forEach { it?.accept(visitor, data) }
        }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        dispatchReceiver = dispatchReceiver?.transform(transformer, data)
        extensionReceiver = extensionReceiver?.transform(transformer, data)
        if (valueArgumentsCount > 0) {
            argumentsByParameterIndex.forEachIndexed { i, irExpression ->
                argumentsByParameterIndex[i] = irExpression?.transform(transformer, data)
            }
        }
    }
}

internal fun IrMemberAccessExpression<*>.throwNoSuchArgumentSlotException(kind: String, index: Int, total: Int): Nothing {
    throw AssertionError(
        "No such $kind argument slot in ${this::class.java.simpleName}: $index (total=$total)" +
                (symbol.signature?.let { ".\nSymbol: $it" } ?: "")
    )
}

fun IrMemberAccessExpression<*>.getTypeArgument(typeParameterDescriptor: TypeParameterDescriptor): IrType? =
    getTypeArgument(typeParameterDescriptor.index)

fun IrMemberAccessExpression<*>.copyTypeArgumentsFrom(other: IrMemberAccessExpression<*>, shift: Int = 0) {
    assert(typeArgumentsCount == other.typeArgumentsCount + shift) {
        "Mismatching type arguments: $typeArgumentsCount vs ${other.typeArgumentsCount} + $shift"
    }
    for (i in 0 until other.typeArgumentsCount) {
        putTypeArgument(i + shift, other.getTypeArgument(i))
    }
}

inline fun IrMemberAccessExpression<*>.putTypeArguments(
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

fun IrMemberAccessExpression<*>.getTypeArgumentOrDefault(irTypeParameter: IrTypeParameter) =
    getTypeArgument(irTypeParameter.index) ?: irTypeParameter.defaultType

fun IrMemberAccessExpression<*>.getValueArgument(valueParameterDescriptor: ValueParameterDescriptor) =
    getValueArgument(valueParameterDescriptor.index)

fun IrMemberAccessExpression<*>.putValueArgument(valueParameterDescriptor: ValueParameterDescriptor, valueArgument: IrExpression?) {
    putValueArgument(valueParameterDescriptor.index, valueArgument)
}

@ObsoleteDescriptorBasedAPI
inline fun <T : IrMemberAccessExpression<*>> T.mapTypeParameters(transform: (TypeParameterDescriptor) -> IrType) : T =
    apply {
        val descriptor = symbol.descriptor as CallableDescriptor
        descriptor.typeParameters.forEach {
            putTypeArgument(it.index, transform(it))
        }
    }

@ObsoleteDescriptorBasedAPI
inline fun <T : IrMemberAccessExpression<*>> T.mapValueParameters(transform: (ValueParameterDescriptor) -> IrExpression?): T =
    apply {
        val descriptor = symbol.descriptor as CallableDescriptor
        descriptor.valueParameters.forEach {
            putValueArgument(it.index, transform(it))
        }
    }

@ObsoleteDescriptorBasedAPI
inline fun <T : IrMemberAccessExpression<*>> T.mapValueParametersIndexed(transform: (Int, ValueParameterDescriptor) -> IrExpression?): T =
    apply {
        val descriptor = symbol.descriptor as CallableDescriptor
        descriptor.valueParameters.forEach {
            putValueArgument(it.index, transform(it.index, it))
        }
    }

fun IrMemberAccessExpression<*>.putArgument(callee: IrFunction, parameter: IrValueParameter, argument: IrExpression) =
    when (parameter) {
        callee.dispatchReceiverParameter -> dispatchReceiver = argument
        callee.extensionReceiverParameter -> extensionReceiver = argument
        else -> putValueArgument(parameter.index, argument)
    }
