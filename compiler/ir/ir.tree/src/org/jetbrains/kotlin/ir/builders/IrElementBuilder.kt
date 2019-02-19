/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET

abstract class IrElementBuilder {

    var startOffset: Int = UNDEFINED_OFFSET
    var endOffset: Int = UNDEFINED_OFFSET

    fun updateFrom(from: IrElement) {
        startOffset = from.startOffset
        endOffset = from.endOffset
    }
}


fun IrElementBuilder.setSourceRange(from: IrElement) {
    startOffset = from.startOffset
    endOffset = from.endOffset
}