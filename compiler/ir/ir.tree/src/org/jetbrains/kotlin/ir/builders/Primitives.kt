/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.ir.builders

import org.jetbrains.kotlin.ir.expressions.IrElseBranch
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType

// TODO rewrite using IR Builders

fun primitiveOp1(
    startOffset: Int, endOffset: Int,
    primitiveOpSymbol: IrSimpleFunctionSymbol,
    primitiveOpReturnType: IrType,
    origin: IrStatementOrigin,
    dispatchReceiver: IrExpression
): IrExpression =
    IrCallImpl(startOffset, endOffset, primitiveOpReturnType, primitiveOpSymbol, origin = origin).also {
        it.dispatchReceiver = dispatchReceiver
    }

fun primitiveOp2(
    startOffset: Int, endOffset: Int,
    primitiveOpSymbol: IrSimpleFunctionSymbol,
    primitiveOpReturnType: IrType,
    origin: IrStatementOrigin,
    argument1: IrExpression, argument2: IrExpression
): IrExpression =
    IrCallImpl(
        startOffset, endOffset,
        primitiveOpReturnType,
        primitiveOpSymbol, typeArgumentsCount = 0,
        valueArgumentsCount = 2,
        origin = origin
    ).apply {
        putValueArgument(0, argument1)
        putValueArgument(1, argument2)
    }

fun IrGeneratorContextInterface.constNull(startOffset: Int, endOffset: Int): IrExpression =
    IrConstImpl.constNull(startOffset, endOffset, irBuiltIns.nothingNType)

fun IrGeneratorContextInterface.equalsNull(startOffset: Int, endOffset: Int, argument: IrExpression): IrExpression =
    primitiveOp2(
        startOffset, endOffset, irBuiltIns.eqeqSymbol, irBuiltIns.booleanType, IrStatementOrigin.EQEQ,
        argument, constNull(startOffset, endOffset)
    )

fun IrGeneratorContextInterface.eqeqeq(startOffset: Int, endOffset: Int, argument1: IrExpression, argument2: IrExpression): IrExpression =
    primitiveOp2(startOffset, endOffset, irBuiltIns.eqeqeqSymbol, irBuiltIns.booleanType, IrStatementOrigin.EQEQEQ, argument1, argument2)

fun IrGeneratorContextInterface.constTrue(startOffset: Int, endOffset: Int) =
    IrConstImpl.constTrue(startOffset, endOffset, irBuiltIns.booleanType)

fun IrGeneratorContextInterface.constFalse(startOffset: Int, endOffset: Int) =
    IrConstImpl.constFalse(startOffset, endOffset, irBuiltIns.booleanType)

fun IrGeneratorContextInterface.elseBranch(elseExpr: IrExpression): IrElseBranch {
    val startOffset = elseExpr.startOffset
    val endOffset = elseExpr.endOffset
    return IrElseBranchImpl(startOffset, endOffset, constTrue(startOffset, endOffset), elseExpr)
}

// a || b == if (a) true else b
fun IrGeneratorContextInterface.oror(
    startOffset: Int,
    endOffset: Int,
    a: IrExpression,
    b: IrExpression,
    origin: IrStatementOrigin = IrStatementOrigin.OROR
): IrWhen =
    IrIfThenElseImpl(startOffset, endOffset, irBuiltIns.booleanType, origin).apply {
        branches.add(IrBranchImpl(a, constTrue(a.startOffset, a.endOffset)))
        branches.add(elseBranch(b))
    }

fun IrGeneratorContextInterface.oror(a: IrExpression, b: IrExpression, origin: IrStatementOrigin = IrStatementOrigin.OROR): IrWhen =
    oror(b.startOffset, b.endOffset, a, b, origin)

fun IrGeneratorContextInterface.whenComma(a: IrExpression, b: IrExpression): IrWhen =
    oror(a, b, IrStatementOrigin.WHEN_COMMA)

// a && b == if (a) b else false
fun IrGeneratorContextInterface.andand(
    startOffset: Int,
    endOffset: Int,
    a: IrExpression,
    b: IrExpression,
    origin: IrStatementOrigin = IrStatementOrigin.ANDAND
): IrWhen =
    IrIfThenElseImpl(startOffset, endOffset, irBuiltIns.booleanType, origin).apply {
        branches.add(IrBranchImpl(a, b))
        branches.add(elseBranch(constFalse(b.startOffset, b.endOffset)))
    }

fun IrGeneratorContextInterface.andand(a: IrExpression, b: IrExpression, origin: IrStatementOrigin = IrStatementOrigin.ANDAND): IrWhen =
    andand(b.startOffset, b.endOffset, a, b, origin)