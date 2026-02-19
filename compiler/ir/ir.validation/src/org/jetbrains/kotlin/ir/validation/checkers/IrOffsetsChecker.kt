/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.validation.checkers.context.CheckerContext

object IrOffsetsChecker : IrElementChecker<IrElement>(IrElement::class) {
    override fun check(element: IrElement, context: CheckerContext) {
        when {
            element.startOffset < -2 || element.endOffset < -2 -> context.error(
                element,
                "Element has invalid offsets. Offsets must be >= 0, UNDEFINED_OFFSET (-1) or SYNTHETIC_OFFSET (-2). " +
                        "Actual: startOffset=${element.startOffset}, endOffset=${element.endOffset}"
            )
            (element.startOffset < 0 || element.endOffset < 0) && element.startOffset != element.endOffset -> context.error(
                element,
                "Element has invalid offsets. UNDEFINED_OFFSET (-1) and SYNTHETIC_OFFSET (-2) " +
                        "can only appear simultaneously in both startOffset and endOffset. " +
                        "Actual: startOffset=${element.startOffset}, endOffset=${element.endOffset}"
            )
            element.startOffset > element.endOffset -> context.error(
                element,
                "Element has invalid offsets. startOffset must not be greater than endOffset " +
                        "Actual: startOffset=${element.startOffset}, endOffset=${element.endOffset}"
            )
        }
    }
}
