/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.types.IrType

val IrFunctionReference.isWithReflection: Boolean
    get() = reflectionTarget != null

val IrFunctionReference.isAdapterWithReflection: Boolean
    get() = reflectionTarget != null && reflectionTarget != symbol

var IrDynamicOperatorExpression.left: IrExpression
    get() = receiver
    set(value) {
        receiver = value
    }

var IrDynamicOperatorExpression.right: IrExpression
    get() = arguments[0]
    set(value) {
        if (arguments.isEmpty())
            arguments.add(value)
        else
            arguments[0] = value
    }

fun IrFunctionAccessExpression.putArgument(parameter: IrValueParameter, argument: IrExpression): Unit =
    putArgument(symbol.owner, parameter, argument)

fun IrVararg.putElement(i: Int, element: IrVarargElement) {
    elements[i] = element
}

fun IrVararg.addElement(varargElement: IrVarargElement) {
    elements.add(varargElement)
}

fun IrStringConcatenation.addArgument(argument: IrExpression) {
    arguments.add(argument)
}

val IrContainerExpression.isTransparentScope: Boolean
    get() = this is IrComposite

fun IrExpression.implicitCastTo(expectedType: IrType?): IrExpression {
    if (expectedType == null) return this

    return IrTypeOperatorCallImpl(startOffset, endOffset, expectedType, IrTypeOperator.IMPLICIT_CAST, expectedType, this)
}

fun IrExpression.isUnchanging(): Boolean =
    this is IrFunctionExpression ||
            (this is IrCallableReference<*> && dispatchReceiver == null && extensionReceiver == null) ||
            this is IrClassReference ||
            this is IrConst<*> ||
            (this is IrGetValue && !symbol.owner.let { it is IrVariable && it.isVar })

fun IrExpression.hasNoSideEffects(): Boolean =
    isUnchanging() || this is IrGetValue

internal fun IrMemberAccessExpression<*>.checkArgumentSlotAccess(kind: String, index: Int, total: Int) {
    if (index >= total) {
        throw AssertionError(
            "No such $kind argument slot in ${this::class.java.simpleName}: $index (total=$total)" +
                    (symbol.signature?.let { ".\nSymbol: $it" } ?: "")
        )
    }
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
