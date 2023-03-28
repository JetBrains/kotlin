/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
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

    protected abstract val typeArguments: Array<IrType?>
    val typeArgumentsCount: Int get() = typeArguments.size

    protected abstract val valueArguments: Array<IrExpression?>
    open val valueArgumentsCount: Int get() = valueArguments.size

    fun getValueArgument(index: Int): IrExpression? {
        if (index >= valueArgumentsCount) {
            throw AssertionError("$this: No such value argument slot: $index")
        }
        return valueArguments[index]
    }

    fun putValueArgument(index: Int, valueArgument: IrExpression?) {
        if (index >= valueArgumentsCount) {
            throw AssertionError("$this: No such value argument slot: $index")
        }
        valueArguments[index] = valueArgument
    }

    fun getTypeArgument(index: Int): IrType? {
        if (index >= typeArgumentsCount) {
            throwNoSuchArgumentSlotException("type", index, typeArgumentsCount)
        }
        return typeArguments[index]
    }

    fun putTypeArgument(index: Int, type: IrType?) {
        if (index >= typeArgumentsCount) {
            throwNoSuchArgumentSlotException("type", index, typeArgumentsCount)
        }
        typeArguments[index] = type
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        dispatchReceiver?.accept(visitor, data)
        extensionReceiver?.accept(visitor, data)
        if (valueArgumentsCount > 0) {
            valueArguments.forEach { it?.accept(visitor, data) }
        }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        dispatchReceiver = dispatchReceiver?.transform(transformer, data)
        extensionReceiver = extensionReceiver?.transform(transformer, data)
        if (valueArgumentsCount > 0) {
            valueArguments.forEachIndexed { i, irExpression ->
                valueArguments[i] = irExpression?.transform(transformer, data)
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

fun IrMemberAccessExpression<*>.copyTypeArgumentsFrom(other: IrMemberAccessExpression<*>, shift: Int = 0) {
    assert(typeArgumentsCount == other.typeArgumentsCount + shift) {
        "Mismatching type arguments: $typeArgumentsCount vs ${other.typeArgumentsCount} + $shift"
    }
    for (i in 0 until other.typeArgumentsCount) {
        putTypeArgument(i + shift, other.getTypeArgument(i))
    }
}

val CallableDescriptor.typeParametersCount: Int
    get() =
        when (this) {
            is PropertyAccessorDescriptor -> correspondingProperty.typeParameters.size
            else -> typeParameters.size
        }

fun IrMemberAccessExpression<*>.putArgument(callee: IrFunction, parameter: IrValueParameter, argument: IrExpression) =
    when (parameter) {
        callee.dispatchReceiverParameter -> dispatchReceiver = argument
        callee.extensionReceiverParameter -> extensionReceiver = argument
        else -> putValueArgument(parameter.index, argument)
    }
