/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator

class IrConstImpl internal constructor(
    @Suppress("UNUSED_PARAMETER") constructorIndicator: IrElementConstructorIndicator?,
    override var startOffset: Int,
    override var endOffset: Int,
    override var type: IrType,
    override val kind: IrConstKind,
    override val value: Any?,
) : IrConst() {
    override var attributeOwnerId: IrElement = this

    companion object {
        fun string(startOffset: Int, endOffset: Int, type: IrType, value: String): IrConstImpl =
            IrConstImpl(null, startOffset, endOffset, type, IrConstKind.String, value)

        fun int(startOffset: Int, endOffset: Int, type: IrType, value: Int): IrConstImpl =
            IrConstImpl(null, startOffset, endOffset, type, IrConstKind.Int, value)

        fun uint(startOffset: Int, endOffset: Int, type: IrType, value: UInt): IrConstImpl =
            IrConstImpl(null, startOffset, endOffset, type, IrConstKind.UInt, value)

        fun constNull(startOffset: Int, endOffset: Int, type: IrType): IrConstImpl =
            IrConstImpl(null, startOffset, endOffset, type, IrConstKind.Null, null)

        fun boolean(startOffset: Int, endOffset: Int, type: IrType, value: Boolean): IrConstImpl =
            IrConstImpl(null, startOffset, endOffset, type, IrConstKind.Boolean, value)

        fun constTrue(startOffset: Int, endOffset: Int, type: IrType): IrConstImpl =
            boolean(startOffset, endOffset, type, true)

        fun constFalse(startOffset: Int, endOffset: Int, type: IrType): IrConstImpl =
            boolean(startOffset, endOffset, type, false)

        fun long(startOffset: Int, endOffset: Int, type: IrType, value: Long): IrConstImpl =
            IrConstImpl(null, startOffset, endOffset, type, IrConstKind.Long, value)

        fun ulong(startOffset: Int, endOffset: Int, type: IrType, value: ULong): IrConstImpl =
            IrConstImpl(null, startOffset, endOffset, type, IrConstKind.ULong, value)

        fun float(startOffset: Int, endOffset: Int, type: IrType, value: Float): IrConstImpl =
            IrConstImpl(null, startOffset, endOffset, type, IrConstKind.Float, value)

        fun double(startOffset: Int, endOffset: Int, type: IrType, value: Double): IrConstImpl =
            IrConstImpl(null, startOffset, endOffset, type, IrConstKind.Double, value)

        fun char(startOffset: Int, endOffset: Int, type: IrType, value: Char): IrConstImpl =
            IrConstImpl(null, startOffset, endOffset, type, IrConstKind.Char, value)

        fun byte(startOffset: Int, endOffset: Int, type: IrType, value: Byte): IrConstImpl =
            IrConstImpl(null, startOffset, endOffset, type, IrConstKind.Byte, value)

        fun ubyte(startOffset: Int, endOffset: Int, type: IrType, value: UByte): IrConstImpl =
            IrConstImpl(null, startOffset, endOffset, type, IrConstKind.UByte, value)

        fun short(startOffset: Int, endOffset: Int, type: IrType, value: Short): IrConstImpl =
            IrConstImpl(null, startOffset, endOffset, type, IrConstKind.Short, value)

        fun ushort(startOffset: Int, endOffset: Int, type: IrType, value: UShort): IrConstImpl =
            IrConstImpl(null, startOffset, endOffset, type, IrConstKind.UShort, value)
    }
}
