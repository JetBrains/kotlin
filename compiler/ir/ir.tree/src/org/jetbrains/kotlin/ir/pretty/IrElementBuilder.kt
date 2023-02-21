/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET

@PrettyIrDsl
abstract class IrElementBuilder<out Element : IrElement> internal constructor() {

    internal var startOffset: Int by SetAtMostOnce(UNDEFINED_OFFSET)
    internal var endOffset: Int by SetAtMostOnce(UNDEFINED_OFFSET)

    @PrettyIrDsl
    open fun debugInfo(startOffset: Int, endOffset: Int) {
        this.startOffset = startOffset
        this.endOffset = endOffset
    }

    internal abstract fun build(): Element
}
