/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.LineAndColumn
import org.jetbrains.kotlin.wasm.ir.WasmExpressionBuilder
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation

enum class LocationType {
    START {
        override fun getLineAndColumnNumberFor(irElement: IrElement, fileEntry: IrFileEntry) =
            fileEntry.getLineAndColumnNumbers(irElement.startOffset)
    },
    END {
        override fun getLineAndColumnNumberFor(irElement: IrElement, fileEntry: IrFileEntry) =
            fileEntry.getLineAndColumnNumbers(irElement.endOffset)
    };

    abstract fun getLineAndColumnNumberFor(irElement: IrElement, fileEntry: IrFileEntry): LineAndColumn
}

fun IrElement.getSourceLocation(fileEntry: IrFileEntry?, type: LocationType = LocationType.START): SourceLocation {
    if (fileEntry == null) return SourceLocation.NoLocation("fileEntry is null")

    val path = fileEntry.name
    val (line, column) = type.getLineAndColumnNumberFor(this, fileEntry)

    if (line < 0 || column < 0) return SourceLocation.NoLocation("startLine or startColumn < 0")

    return SourceLocation.Location(path, line, column)
}

fun WasmExpressionBuilder.buildUnreachableForVerifier() {
    buildUnreachable(SourceLocation.NoLocation("This instruction should never be reached, but required for wasm verifier"))
}

fun WasmExpressionBuilder.buildUnreachableAfterNothingType() {
    buildUnreachable(
        SourceLocation.NoLocation(
            "The unreachable instruction after an expression with Nothing type to make sure that " +
                    "execution doesn't come here (or it fails fast if so). It also might be required for wasm verifier."
        )
    )
}
