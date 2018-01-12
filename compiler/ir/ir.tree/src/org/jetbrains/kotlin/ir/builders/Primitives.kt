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

import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol

fun primitiveOp1(
    startOffset: Int, endOffset: Int,
    primitiveOpSymbol: IrSimpleFunctionSymbol,
    origin: IrStatementOrigin,
    argument: IrExpression
): IrExpression =
    IrUnaryPrimitiveImpl(startOffset, endOffset, origin, primitiveOpSymbol, argument)

fun primitiveOp2(
    startOffset: Int, endOffset: Int,
    primitiveOpSymbol: IrSimpleFunctionSymbol,
    origin: IrStatementOrigin,
    argument1: IrExpression, argument2: IrExpression
): IrExpression =
    IrBinaryPrimitiveImpl(startOffset, endOffset, origin, primitiveOpSymbol, argument1, argument2)

fun IrGeneratorContext.constNull(startOffset: Int, endOffset: Int): IrExpression =
    IrConstImpl.constNull(startOffset, endOffset, builtIns.nullableNothingType)

fun IrGeneratorContext.equalsNull(startOffset: Int, endOffset: Int, argument: IrExpression): IrExpression =
    primitiveOp2(
        startOffset, endOffset, irBuiltIns.eqeqSymbol, IrStatementOrigin.EQEQ,
        argument, constNull(startOffset, endOffset)
    )

fun IrGeneratorContext.eqeqeq(startOffset: Int, endOffset: Int, argument1: IrExpression, argument2: IrExpression): IrExpression =
    primitiveOp2(startOffset, endOffset, irBuiltIns.eqeqeqSymbol, IrStatementOrigin.EQEQEQ, argument1, argument2)

fun IrGeneratorContext.throwNpe(startOffset: Int, endOffset: Int, origin: IrStatementOrigin): IrExpression =
    IrNullaryPrimitiveImpl(startOffset, endOffset, origin, irBuiltIns.throwNpeSymbol)

// a || b == if (a) true else b
fun IrGeneratorContext.oror(
    startOffset: Int,
    endOffset: Int,
    a: IrExpression,
    b: IrExpression,
    origin: IrStatementOrigin = IrStatementOrigin.OROR
): IrWhen =
    IrIfThenElseImpl(
        startOffset, endOffset, builtIns.booleanType,
        a, IrConstImpl.constTrue(b.startOffset, b.endOffset, builtIns.booleanType), b,
        origin
    )

fun IrGeneratorContext.oror(a: IrExpression, b: IrExpression, origin: IrStatementOrigin = IrStatementOrigin.OROR): IrWhen =
    oror(b.startOffset, b.endOffset, a, b, origin)

fun IrGeneratorContext.whenComma(a: IrExpression, b: IrExpression): IrWhen =
    oror(a, b, IrStatementOrigin.WHEN_COMMA)

// a && b == if (a) b else false
fun IrGeneratorContext.andand(
    startOffset: Int,
    endOffset: Int,
    a: IrExpression,
    b: IrExpression,
    origin: IrStatementOrigin = IrStatementOrigin.ANDAND
): IrWhen =
    IrIfThenElseImpl(
        startOffset, endOffset, builtIns.booleanType,
        a, b, IrConstImpl.constFalse(b.startOffset, b.endOffset, builtIns.booleanType),
        origin
    )

fun IrGeneratorContext.andand(a: IrExpression, b: IrExpression, origin: IrStatementOrigin = IrStatementOrigin.ANDAND): IrWhen =
    andand(b.startOffset, b.endOffset, a, b, origin)