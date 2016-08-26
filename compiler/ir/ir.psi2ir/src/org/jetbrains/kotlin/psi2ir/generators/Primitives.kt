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

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.*

fun primitiveOp1(startOffset: Int, endOffset: Int, primitiveOpDescriptor: CallableDescriptor, irOperator: IrOperator,
                 argument: IrExpression): IrExpression =
        IrUnaryPrimitiveImpl(startOffset, endOffset, irOperator, primitiveOpDescriptor, argument)

fun primitiveOp2(startOffset: Int, endOffset: Int, primitiveOpDescriptor: CallableDescriptor, irOperator: IrOperator,
                 argument1: IrExpression, argument2: IrExpression): IrExpression =
        IrBinaryPrimitiveImpl(startOffset, endOffset, irOperator, primitiveOpDescriptor, argument1, argument2)

fun GeneratorContext.constNull(startOffset: Int, endOffset: Int): IrExpression =
        IrConstImpl.constNull(startOffset, endOffset, builtIns.nullableNothingType)

fun GeneratorContext.equalsNull(startOffset: Int, endOffset: Int, argument: IrExpression): IrExpression =
        primitiveOp2(startOffset, endOffset, irBuiltIns.eqeq, IrOperator.EQEQ,
                     argument, constNull(startOffset, endOffset))

fun GeneratorContext.throwNpe(startOffset: Int, endOffset: Int, operator: IrOperator): IrExpression =
        IrNullaryPrimitiveImpl(startOffset, endOffset, operator, irBuiltIns.throwNpe)

// a || b == if (a) true else b
fun GeneratorContext.oror(startOffset: Int, endOffset: Int, a: IrExpression, b: IrExpression, operator: IrOperator = IrOperator.OROR): IrWhen =
        IrIfThenElseImpl(startOffset, endOffset, builtIns.booleanType,
                         a, IrConstImpl.constTrue(b.startOffset, b.endOffset, b.type), b,
                         operator)

fun GeneratorContext.oror(a: IrExpression, b: IrExpression, operator: IrOperator = IrOperator.OROR): IrWhen =
        oror(b.startOffset, b.endOffset, a, b, operator)

fun GeneratorContext.whenComma(a: IrExpression, b: IrExpression): IrWhen =
        oror(a, b, IrOperator.WHEN_COMMA)

// a && b == if (a) b else false
fun GeneratorContext.andand(startOffset: Int, endOffset: Int, a: IrExpression, b: IrExpression, operator: IrOperator = IrOperator.ANDAND): IrWhen =
        IrIfThenElseImpl(startOffset, endOffset, builtIns.booleanType,
                         a, b, IrConstImpl.constFalse(b.startOffset, b.endOffset, b.type),
                         operator)

fun GeneratorContext.andand(a: IrExpression, b: IrExpression, operator: IrOperator = IrOperator.ANDAND): IrWhen =
        andand(b.startOffset, b.endOffset, a, b, operator)