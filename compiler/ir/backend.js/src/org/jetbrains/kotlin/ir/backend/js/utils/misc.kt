/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isNullableAny
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.isTopLevelDeclaration
import org.jetbrains.kotlin.name.Name

fun TODO(element: IrElement): Nothing = TODO(element::class.java.simpleName + " is not supported yet here")

fun IrFunction.isEqualsInheritedFromAny() =
    name == Name.identifier("equals") &&
            dispatchReceiverParameter != null &&
            valueParameters.size == 1 &&
            valueParameters[0].type.isNullableAny()

fun IrDeclaration.hasStaticDispatch() = when (this) {
    is IrSimpleFunction -> dispatchReceiverParameter == null
    is IrProperty -> isTopLevelDeclaration
    is IrField -> isStatic
    else -> true
}

fun List<IrExpression>.toJsArrayLiteral(context: JsIrBackendContext, arrayType: IrType, elementType: IrType): IrExpression {
    val irVararg = IrVarargImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, arrayType, elementType, this)

    return IrCallImpl(
        UNDEFINED_OFFSET, UNDEFINED_OFFSET, arrayType,
        context.intrinsics.arrayLiteral,
        valueArgumentsCount = 1,
        typeArgumentsCount = 0
    ).apply {
        putValueArgument(0, irVararg)
    }
}

// TODO: support more cases like built-in operator call and so on

fun IrExpression?.isPure(anyVariable: Boolean, checkFields: Boolean = true): Boolean {
    if (this == null) return true

    fun IrExpression.isPureImpl(): Boolean {
        return when (this) {
            is IrConst<*> -> true
            is IrGetValue -> {
                if (anyVariable) return true
                val valueDeclaration = symbol.owner
                if (valueDeclaration is IrVariable) !valueDeclaration.isVar
                else true
            }
            is IrGetObjectValue -> type.isUnit()
            else -> false
        }
    }

    if (isPureImpl()) return true

    if (!checkFields) return false

    if (this is IrGetField) {
        if (!symbol.owner.isFinal) {
            if (!anyVariable) {
                return false
            }
        }
        return receiver.isPure(anyVariable)
    }

    return false
}