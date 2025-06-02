/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders.v2

import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl

context(context: IrBuiltInsAware)
fun IrBuilderNew.irEqualsNull(argument: IrExpression) =
    irEquals(argument, irNull())

context(context: IrBuiltInsAware)
fun IrBuilderNew.irNot(arg: IrExpression) =
    irCall(
        context.irBuiltIns.booleanNotSymbol,
        context.irBuiltIns.booleanType,
        origin = IrStatementOrigin.EXCL
    ).also {
        it.arguments[0] = arg
    }


context(context: IrBuiltInsAware)
fun IrBuilderNew.irEquals(arg1: IrExpression, arg2: IrExpression, origin: IrStatementOrigin = IrStatementOrigin.EQEQ): IrCall =
    irCall(
        context.irBuiltIns.eqeqSymbol,
        context.irBuiltIns.booleanType,
        origin = origin
    ).apply {
        arguments[0] = arg1
        arguments[1] = arg2
    }

context(context: IrBuiltInsAware)
fun IrBuilderNew.irEqEqEq(arg1: IrExpression, arg2: IrExpression, origin: IrStatementOrigin = IrStatementOrigin.EQEQEQ): IrCall =
    irCall(
        context.irBuiltIns.eqeqeqSymbol,
        context.irBuiltIns.booleanType,
        origin = origin
    ).apply {
        arguments[0] = arg1
        arguments[1] = arg2
    }

context(context: IrBuiltInsAware)
fun IrBuilderNew.irNotEquals(arg1: IrExpression, arg2: IrExpression) =
    irNot(irEquals(arg1, arg2))


context(context: IrBuiltInsAware)
fun IrBuilderNew.irEqEqEq(argument1: IrExpression, argument2: IrExpression): IrExpression =
    irCall(
        context.irBuiltIns.eqeqeqSymbol,
        context.irBuiltIns.booleanType,
        origin = IrStatementOrigin.EQEQEQ
    ).apply {
        arguments[0] = argument1
        arguments[1] = argument2
    }

// a || b == if (a) true else b
context(context: IrBuiltInsAware)
fun IrBuilderNew.irOrOr(
    a: IrExpression,
    b: IrExpression,
    origin: IrStatementOrigin = IrStatementOrigin.OROR
): IrWhen =
    irWhen(context.irBuiltIns.booleanType, origin) {
        +irBranch(a, buildIrAt(a) { irTrue() })
        +irElseBranch(b)
    }


// a && b == if (a) b else false
context(context: IrBuiltInsAware)
fun IrBuilderNew.irAndAnd(
    a: IrExpression,
    b: IrExpression,
    origin: IrStatementOrigin = IrStatementOrigin.ANDAND
): IrWhen = irWhen(context.irBuiltIns.booleanType, origin) {
    +irBranch(a, b)
    +irElseBranch(buildIrAt(b) { irFalse() })
}
