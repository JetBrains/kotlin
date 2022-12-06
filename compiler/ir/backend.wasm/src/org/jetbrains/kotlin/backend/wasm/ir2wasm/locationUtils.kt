/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation

fun IrExpression.getSourceLocation(fileEntry: IrFileEntry?): SourceLocation {
    if (fileEntry == null) return SourceLocation.NoLocation

    val path = fileEntry.name
    val startLine = fileEntry.getLineNumber(startOffset)
    val startColumn = fileEntry.getColumnNumber(startOffset)

    if (startLine < 0 || startColumn < 0) return SourceLocation.NoLocation

    return SourceLocation.Location(path, startLine, startColumn)
}
