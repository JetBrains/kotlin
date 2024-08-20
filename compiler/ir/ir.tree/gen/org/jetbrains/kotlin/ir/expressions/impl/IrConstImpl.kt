/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator

class IrConstImpl internal constructor(
    @Suppress("UNUSED_PARAMETER") constructorIndicator: IrElementConstructorIndicator?,
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    override var kind: IrConstKind,
    override var value: Any?,
) : IrConst() {
    override var attributeOwnerId: IrAttributeContainer = this

    override var originalBeforeInline: IrAttributeContainer? = null

    companion object {
        fun string(startOffset: Int, endOffset: Int, type: IrType, value: String): IrConstImpl =
            IrConstImpl(startOffset, endOffset, type, IrConstKind.String, value)

        fun int(startOffset: Int, endOffset: Int, type: IrType, value: Int): IrConstImpl =
            IrConstImpl(startOffset, endOffset, type, IrConstKind.Int, value)

        fun constNull(startOffset: Int, endOffset: Int, type: IrType): IrConstImpl =
            IrConstImpl(startOffset, endOffset, type, IrConstKind.Null, null)

        fun boolean(startOffset: Int, endOffset: Int, type: IrType, value: Boolean): IrConstImpl =
            IrConstImpl(startOffset, endOffset, type, IrConstKind.Boolean, value)

        fun constTrue(startOffset: Int, endOffset: Int, type: IrType): IrConstImpl =
            boolean(startOffset, endOffset, type, true)

        fun constFalse(startOffset: Int, endOffset: Int, type: IrType): IrConstImpl =
            boolean(startOffset, endOffset, type, false)

        fun long(startOffset: Int, endOffset: Int, type: IrType, value: Long): IrConstImpl =
            IrConstImpl(startOffset, endOffset, type, IrConstKind.Long, value)

        fun float(startOffset: Int, endOffset: Int, type: IrType, value: Float): IrConstImpl =
            IrConstImpl(startOffset, endOffset, type, IrConstKind.Float, value)

        fun double(startOffset: Int, endOffset: Int, type: IrType, value: Double): IrConstImpl =
            IrConstImpl(startOffset, endOffset, type, IrConstKind.Double, value)

        fun char(startOffset: Int, endOffset: Int, type: IrType, value: Char): IrConstImpl =
            IrConstImpl(startOffset, endOffset, type, IrConstKind.Char, value)

        fun byte(startOffset: Int, endOffset: Int, type: IrType, value: Byte): IrConstImpl =
            IrConstImpl(startOffset, endOffset, type, IrConstKind.Byte, value)

        fun short(startOffset: Int, endOffset: Int, type: IrType, value: Short): IrConstImpl =
            IrConstImpl(startOffset, endOffset, type, IrConstKind.Short, value)
    }
}
