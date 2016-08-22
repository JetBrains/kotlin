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
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.assertCast
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.SmartList

class IrBlockBuilder(val startOffset: Int, val endOffset: Int, val irOperator: IrOperator, val generator: IrBodyGenerator) {
    private val statements = SmartList<IrStatement>()
    private var resultType: KotlinType? = null
    private var hasResult = false
    val scope: Scope get() = generator.scope

    fun <T : IrStatement?> add(irStatement: T): T {
        if (irStatement != null) {
            statements.add(irStatement)
        }
        return irStatement
    }

    fun <T : IrExpression> result(irExpression: T): T {
        resultType = irExpression.type
        hasResult = true
        statements.add(irExpression)
        return irExpression
    }

    fun build() =
            if (statements.size == 1)
                statements[0]
            else {
                val block = IrBlockImpl(startOffset, endOffset, resultType, hasResult, irOperator)
                statements.forEach { block.addStatement(it) }
                block
            }
}

fun IrBodyGenerator.block(ktElement: KtElement, irOperator: IrOperator, body: IrBlockBuilder.() -> Unit): IrExpression =
        IrBlockBuilder(ktElement.startOffset, ktElement.endOffset, irOperator, this).apply(body).build().assertCast()

fun IrBodyGenerator.block(irExpression: IrExpression, irOperator: IrOperator, body: IrBlockBuilder.() -> Unit): IrExpression =
        IrBlockBuilder(irExpression.startOffset, irExpression.endOffset, irOperator, this).apply(body).build().assertCast()

fun IrBlockBuilder.constType(constKind: IrConstKind<*>): KotlinType =
        when (constKind) {
            IrConstKind.Null -> generator.context.builtIns.nullableNothingType
            IrConstKind.Boolean -> generator.context.builtIns.anyType
            IrConstKind.Byte -> generator.context.builtIns.byteType
            IrConstKind.Short -> generator.context.builtIns.shortType
            IrConstKind.Int -> generator.context.builtIns.intType
            IrConstKind.Long -> generator.context.builtIns.longType
            IrConstKind.String -> generator.context.builtIns.stringType
            IrConstKind.Float -> generator.context.builtIns.floatType
            IrConstKind.Double -> generator.context.builtIns.doubleType
        }

fun <T> IrBlockBuilder.const(constKind: IrConstKind<T>, value: T) =
        IrConstImpl(startOffset, endOffset, constType(constKind), constKind, value)

fun IrBlockBuilder.constNull() =
        const(IrConstKind.Null, null)

fun IrBlockBuilder.op0(operatorDescriptor: CallableDescriptor) =
        IrNullaryOperatorImpl(startOffset, endOffset, irOperator, operatorDescriptor)

fun IrBlockBuilder.op1(operatorDescriptor: CallableDescriptor, argument: IrExpression) =
        IrUnaryOperatorImpl(startOffset, endOffset, irOperator, operatorDescriptor, argument)

fun IrBlockBuilder.op2(operatorDescriptor: CallableDescriptor, argument1: IrExpression, argument2: IrExpression) =
        IrBinaryOperatorImpl(startOffset, endOffset, irOperator, operatorDescriptor, argument1, argument2)

fun IrBlockBuilder.equalsNull(argument: IrExpression) =
        op2(generator.context.irBuiltIns.eqeq, argument, constNull())

fun IrBlockBuilder.ifThenElse(type: KotlinType?, condition: IrExpression, thenBranch: IrExpression, elseBranch: IrExpression) =
        IrIfThenElseImpl(startOffset, endOffset, type, condition, thenBranch, elseBranch, irOperator)
