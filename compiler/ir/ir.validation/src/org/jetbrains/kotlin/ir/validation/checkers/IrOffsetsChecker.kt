/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.validation.checkers.context.CheckerContext

object IrOffsetsChecker : IrElementChecker<IrElement>(IrElement::class) {
    override fun check(element: IrElement, context: CheckerContext) {
        if (element.startOffset > element.endOffset) {
            context.error(element, "Element has invalid offsets: startOffset=${element.startOffset}, endOffset=${element.endOffset}")
        }
    }
}
