/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.context

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrInlinedFunctionBlock
import org.jetbrains.kotlin.ir.validation.temporarilyPushing

object OffsetRangeChainUpdater : ContextUpdater {
    override fun runInNewContext(
        context: CheckerContext,
        element: IrElement,
        block: () -> Unit,
    ) {
        OffsetRange.createIfRealValidOffsets(element, useOwnOffsetsOfInlinedFunctionBlock = false)?.let { newOffsetBoundaries ->
            context.offsetRanges.temporarilyPushing(newOffsetBoundaries) {
                block()
            }
        } ?: block()
    }
}

class OffsetRange private constructor(val owner: IrElement, val startOffset: Int, val endOffset: Int) {
    operator fun contains(other: OffsetRange): Boolean = startOffset <= other.startOffset && endOffset >= other.endOffset

    override fun toString() = "[$startOffset:$endOffset]"

    companion object {
        fun createIfRealValidOffsets(element: IrElement, useOwnOffsetsOfInlinedFunctionBlock: Boolean): OffsetRange? =
            if (useOwnOffsetsOfInlinedFunctionBlock || element !is IrInlinedFunctionBlock)
                createIfRealValidOffsets(element, element.startOffset, element.endOffset)
            else
                createIfRealValidOffsets(element, element.inlinedFunctionStartOffset, element.inlinedFunctionEndOffset)

        private fun createIfRealValidOffsets(owner: IrElement, startOffset: Int, endOffset: Int): OffsetRange? =
            if (startOffset in 0..endOffset) OffsetRange(owner, startOffset, endOffset) else null
    }
}
