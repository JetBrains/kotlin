/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.expressions.IrConstantArray
import org.jetbrains.kotlin.ir.expressions.IrConstantValue
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.utils.SmartList

class IrConstantArrayImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    initElements: List<IrConstantValue>,
) : IrConstantArray() {
    override val elements = SmartList(initElements)

    override var attributeOwnerId: IrAttributeContainer = this
    override var originalBeforeInline: IrAttributeContainer? = null

    override fun contentEquals(other: IrConstantValue): Boolean =
        other is IrConstantArray &&
                other.type == type &&
                elements.size == other.elements.size &&
                elements.indices.all { elements[it].contentEquals(other.elements[it]) }

    override fun contentHashCode(): Int {
        var res = type.hashCode()
        for (value in elements) {
            res = res * 31 + value.contentHashCode()
        }
        return res
    }
}