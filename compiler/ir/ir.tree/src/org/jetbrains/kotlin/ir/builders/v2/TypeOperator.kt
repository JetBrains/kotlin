/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders.v2

import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.types.IrType

internal fun IrBuilderNew.irTypeOperatorCall(
    argument: IrExpression,
    typeOperand: IrType,
    resultType: IrType,
    typeOperator: IrTypeOperator
) : IrTypeOperatorCall =
    IrTypeOperatorCallImpl(startOffset, endOffset, resultType, typeOperator, typeOperand, argument)

context(context: IrBuiltInsAware)
fun IrBuilderNew.irIs(argument: IrExpression, type: IrType) =
    irTypeOperatorCall(argument, type, context.irBuiltIns.booleanType, IrTypeOperator.INSTANCEOF)

context(context: IrBuiltInsAware)
fun IrBuilderNew.irNotIs(argument: IrExpression, type: IrType) =
    irTypeOperatorCall(argument, type, context.irBuiltIns.booleanType, IrTypeOperator.NOT_INSTANCEOF)

fun IrBuilderNew.irAs(argument: IrExpression, type: IrType) =
    irTypeOperatorCall(argument, type, type, IrTypeOperator.CAST)

fun IrBuilderNew.irImplicitCast(argument: IrExpression, type: IrType) =
    irTypeOperatorCall(argument, type, type, IrTypeOperator.IMPLICIT_CAST)

fun IrBuilderNew.irReinterpretCast(argument: IrExpression, type: IrType) =
    irTypeOperatorCall(argument, type, type, IrTypeOperator.REINTERPRET_CAST)

fun IrBuilderNew.irSamConversion(argument: IrExpression, type: IrType) =
    irTypeOperatorCall(argument, type, type, IrTypeOperator.SAM_CONVERSION)

context(context: IrBuiltInsAware)
fun IrBuilderNew.irImplicitCoercionToUnit(arg: IrExpression) : IrTypeOperatorCall {
    return irTypeOperatorCall(
        arg,
        context.irBuiltIns.unitType,
        context.irBuiltIns.unitType,
        IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
    )
}

context(context: IrBuiltInsAware)
fun IrBuilderNew.irImplicitCoercionToUnitIfNeeded(arg: IrExpression) : IrExpression {
    if (arg.type == context.irBuiltIns.unitType) return arg
    return irImplicitCoercionToUnit(arg)
}

fun IrBuilderNew.irImplicitCastIfNeeded(arg: IrExpression, type: IrType) : IrExpression {
    if (arg.type == type) return arg
    return irImplicitCast(arg, type)
}