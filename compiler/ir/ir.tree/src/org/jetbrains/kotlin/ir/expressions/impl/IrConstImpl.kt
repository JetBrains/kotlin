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

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getPrimitiveType
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.makeNullable

class IrConstImpl<T>(
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    override var kind: IrConstKind<T>,
    override var value: T
) : IrConst<T>() {
    override var attributeOwnerId: IrAttributeContainer = this
    override var originalBeforeInline: IrAttributeContainer? = null

    companion object {
        fun string(startOffset: Int, endOffset: Int, type: IrType, value: String): IrConstImpl<String> =
            IrConstImpl(startOffset, endOffset, type, IrConstKind.String, value)

        fun int(startOffset: Int, endOffset: Int, type: IrType, value: Int): IrConstImpl<Int> =
            IrConstImpl(startOffset, endOffset, type, IrConstKind.Int, value)

        fun constNull(startOffset: Int, endOffset: Int, type: IrType): IrConstImpl<Nothing?> =
            IrConstImpl(startOffset, endOffset, type, IrConstKind.Null, null)

        fun boolean(startOffset: Int, endOffset: Int, type: IrType, value: Boolean): IrConstImpl<Boolean> =
            IrConstImpl(startOffset, endOffset, type, IrConstKind.Boolean, value)

        fun constTrue(startOffset: Int, endOffset: Int, type: IrType): IrConstImpl<Boolean> =
            boolean(startOffset, endOffset, type, true)

        fun constFalse(startOffset: Int, endOffset: Int, type: IrType): IrConstImpl<Boolean> =
            boolean(startOffset, endOffset, type, false)

        fun long(startOffset: Int, endOffset: Int, type: IrType, value: Long): IrConstImpl<Long> =
            IrConstImpl(startOffset, endOffset, type, IrConstKind.Long, value)

        fun float(startOffset: Int, endOffset: Int, type: IrType, value: Float): IrConstImpl<Float> =
            IrConstImpl(startOffset, endOffset, type, IrConstKind.Float, value)

        fun double(startOffset: Int, endOffset: Int, type: IrType, value: Double): IrConstImpl<Double> =
            IrConstImpl(startOffset, endOffset, type, IrConstKind.Double, value)

        fun char(startOffset: Int, endOffset: Int, type: IrType, value: Char): IrConstImpl<Char> =
            IrConstImpl(startOffset, endOffset, type, IrConstKind.Char, value)

        fun byte(startOffset: Int, endOffset: Int, type: IrType, value: Byte): IrConstImpl<Byte> =
            IrConstImpl(startOffset, endOffset, type, IrConstKind.Byte, value)

        fun short(startOffset: Int, endOffset: Int, type: IrType, value: Short): IrConstImpl<Short> =
            IrConstImpl(startOffset, endOffset, type, IrConstKind.Short, value)

        fun defaultValueForType(startOffset: Int, endOffset: Int, type: IrType): IrConstImpl<*> {
            if (type.isMarkedNullable()) return constNull(startOffset, endOffset, type)
            return when (type.getPrimitiveType()) {
                PrimitiveType.BOOLEAN -> boolean(startOffset, endOffset, type, false)
                PrimitiveType.CHAR -> char(startOffset, endOffset, type, 0.toChar())
                PrimitiveType.BYTE -> byte(startOffset, endOffset, type, 0)
                PrimitiveType.SHORT -> short(startOffset, endOffset, type, 0)
                PrimitiveType.INT -> int(startOffset, endOffset, type, 0)
                PrimitiveType.FLOAT -> float(startOffset, endOffset, type, 0.0F)
                PrimitiveType.LONG -> long(startOffset, endOffset, type, 0)
                PrimitiveType.DOUBLE -> double(startOffset, endOffset, type, 0.0)
                else -> constNull(startOffset, endOffset, type.makeNullable())
            }
        }
    }
}

fun <T> IrConst<T>.copyWithOffsets(startOffset: Int, endOffset: Int) =
    IrConstImpl(startOffset, endOffset, type, kind, value)
