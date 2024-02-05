/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstantPrimitive
import org.jetbrains.kotlin.ir.expressions.IrConstantValue

class IrConstantPrimitiveImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var value: IrConst<*>,
) : IrConstantPrimitive() {
    override fun contentEquals(other: IrConstantValue) =
        other is IrConstantPrimitive &&
                type == other.type &&
                value.type == other.value.type &&
                value.kind == other.value.kind &&
                value.value == other.value.value

    override fun contentHashCode(): Int {
        var result = type.hashCode()
        result = result * 31 + value.type.hashCode()
        result = result * 31 + value.kind.hashCode()
        result = result * 31 + value.value.hashCode()
        return result
    }

    override var type = value.type
}