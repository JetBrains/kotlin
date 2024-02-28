/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.IrVarargElement
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator
import org.jetbrains.kotlin.utils.SmartList

class IrVarargImpl internal constructor(
    @Suppress("UNUSED_PARAMETER")
    constructorIndicator: IrElementConstructorIndicator?,
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    override var varargElementType: IrType,
) : IrVararg() {
    override val elements: MutableList<IrVarargElement> = SmartList()

    override var attributeOwnerId: IrAttributeContainer = this
    override var originalBeforeInline: IrAttributeContainer? = null
}

fun IrVarargImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    varargElementType: IrType,
) = IrVarargImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    varargElementType = varargElementType,
)

fun IrVarargImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    varargElementType: IrType,
    elements: List<IrVarargElement>,
) = IrVarargImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    varargElementType = varargElementType,
).apply {
    this.elements.addAll(elements)
}