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

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType

interface IrOperatorExpression : IrExpression {
    val operator: IrOperator
    val relatedDescriptor: FunctionDescriptor?
}

interface IrUnaryOperatorExpression : IrOperatorExpression, IrCompoundExpression1

interface IrBinaryOperatorExpression : IrOperatorExpression, IrCompoundExpression2

class IrUnaryOperatorExpressionImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType?,
        override val operator: IrOperator,
        override val relatedDescriptor: FunctionDescriptor?
) : IrCompoundExpression1Base(startOffset, endOffset, type), IrUnaryOperatorExpression {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitUnaryOperator(this, data)
    }
}

class IrBinaryOperatorExpressionImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType?,
        override val operator: IrOperator,
        override val relatedDescriptor: FunctionDescriptor?
) : IrCompoundExpression2Base(startOffset, endOffset, type), IrBinaryOperatorExpression {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitBinaryOperator(this, data)
}
