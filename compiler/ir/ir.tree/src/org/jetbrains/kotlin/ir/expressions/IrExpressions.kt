/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.CompilerVersionOfApiDeprecation
import org.jetbrains.kotlin.DeprecatedCompilerApi
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.render

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

@DeprecatedCompilerApi(CompilerVersionOfApiDeprecation._2_1_20)
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
            (this is IrCallableReference<*> && arguments.all { it == null }) ||
            this is IrClassReference ||
            this is IrConst ||
            (this is IrGetValue && !symbol.owner.let { it is IrVariable && it.isVar })

fun IrExpression.hasNoSideEffects(): Boolean =
    isUnchanging() || this is IrGetValue

internal fun IrMemberAccessExpression<*>.checkArgumentSlotAccess(kind: String, index: Int, total: Int) {
    if (index >= total) {
        // KT-69558: TODO convert this throw to `irError(...) { withIrEntry(this) }`
        throw AssertionError(
            "No such $kind argument slot in ${this::class.java.simpleName}: $index (total=$total)" +
                    (symbol.signature?.let { ".\nSymbol: $it" } ?: "") +
                    "\nExpression: ${render()}"
        )
    }
}

fun IrMemberAccessExpression<*>.copyTypeArgumentsFrom(other: IrMemberAccessExpression<*>, shift: Int = 0) {
    assert(this.typeArguments.size == other.typeArguments.size + shift) {
        "Mismatching type arguments: ${this.typeArguments.size} vs ${other.typeArguments.size} + $shift"
    }
    for (i in other.typeArguments.indices) {
        this.typeArguments[i + shift] = other.typeArguments[i]
    }
}

val CallableDescriptor.typeParametersCount: Int
    get() =
        when (this) {
            is PropertyAccessorDescriptor -> correspondingProperty.typeParameters.size
            else -> typeParameters.size
        }

@DeprecatedCompilerApi(CompilerVersionOfApiDeprecation._2_1_20)
fun IrMemberAccessExpression<*>.putArgument(
    @Suppress("unused") callee: IrFunction, // To be removed
    parameter: IrValueParameter,
    argument: IrExpression
) {
    arguments[parameter.indexInParameters] = argument
}
