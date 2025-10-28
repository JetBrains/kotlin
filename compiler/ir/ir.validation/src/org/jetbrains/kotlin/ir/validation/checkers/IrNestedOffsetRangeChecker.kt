/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.validation.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.validation.checkers.context.ContextUpdater
import org.jetbrains.kotlin.ir.validation.checkers.context.OffsetRange
import org.jetbrains.kotlin.ir.validation.checkers.context.OffsetRangeChainUpdater

object IrNestedOffsetRangeChecker : IrElementChecker<IrElement>(IrElement::class) {
    override val requiredContextUpdaters: Set<ContextUpdater>
        get() = setOf(OffsetRangeChainUpdater)

    override fun check(
        element: IrElement,
        context: CheckerContext,
    ) {
        val outerRange = context.offsetRanges.lastOrNull() ?: return
        val currentRange = OffsetRange.createIfRealValidOffsets(element, useOwnOffsetsOfInlinedFunctionBlock = true) ?: return

        if (currentRange !in outerRange)
            context.error(
                element,
                "The offsets range $currentRange is not within the outer offsets range $outerRange (owner = ${outerRange.owner.render()})"
            )
    }
}
