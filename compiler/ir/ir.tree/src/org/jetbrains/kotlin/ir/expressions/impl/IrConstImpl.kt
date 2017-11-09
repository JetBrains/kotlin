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

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType

class IrConstImpl<T> (
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        override val kind: IrConstKind<T>,
        override val value: T
) : IrTerminalExpressionBase(startOffset, endOffset, type), IrConst<T> {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitConst(this, data)

    override fun copy(): IrConst<T> =
            IrConstImpl(startOffset, endOffset, type, kind, value)

    companion object {
        fun string(startOffset: Int, endOffset: Int, type: KotlinType, value: String): IrConstImpl<String> =
                IrConstImpl(startOffset, endOffset, type, IrConstKind.String, value)

        fun int(startOffset: Int, endOffset: Int, type: KotlinType, value: Int): IrConstImpl<Int> =
                IrConstImpl(startOffset, endOffset, type, IrConstKind.Int, value)

        fun constNull(startOffset: Int, endOffset: Int, type: KotlinType): IrConstImpl<Nothing?> =
                IrConstImpl(startOffset, endOffset, type, IrConstKind.Null, null)

        fun boolean(startOffset: Int, endOffset: Int, type: KotlinType, value: Boolean): IrConstImpl<Boolean> =
                IrConstImpl(startOffset, endOffset, type, IrConstKind.Boolean, value)

        fun constTrue(startOffset: Int, endOffset: Int, type: KotlinType): IrConstImpl<Boolean> =
                boolean(startOffset, endOffset, type, true)

        fun constFalse(startOffset: Int, endOffset: Int, type: KotlinType): IrConstImpl<Boolean> =
                boolean(startOffset, endOffset, type, false)

        fun long(startOffset: Int, endOffset: Int, type: KotlinType, value: Long): IrExpression =
                IrConstImpl(startOffset, endOffset, type, IrConstKind.Long, value)

        fun float(startOffset: Int, endOffset: Int, type: KotlinType, value: Float): IrExpression =
                IrConstImpl(startOffset, endOffset, type, IrConstKind.Float, value)

        fun double(startOffset: Int, endOffset: Int, type: KotlinType, value: Double): IrExpression =
                IrConstImpl(startOffset, endOffset, type, IrConstKind.Double, value)

        fun char(startOffset: Int, endOffset: Int, type: KotlinType, value: Char): IrExpression =
                IrConstImpl(startOffset, endOffset, type, IrConstKind.Char, value)

        fun byte(startOffset: Int, endOffset: Int, type: KotlinType, value: Byte): IrExpression =
                IrConstImpl(startOffset, endOffset, type, IrConstKind.Byte, value)

        fun short(startOffset: Int, endOffset: Int, type: KotlinType, value: Short): IrExpression =
                IrConstImpl(startOffset, endOffset, type, IrConstKind.Short, value)
    }
}