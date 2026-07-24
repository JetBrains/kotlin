/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.LineAndColumn
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.isInlinedCode
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.shouldIgnore
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation

private val IrElement.hasSyntheticOrUndefinedLocation: Boolean
    get() = startOffset in SYNTHETIC_OFFSET..UNDEFINED_OFFSET ||
            endOffset in SYNTHETIC_OFFSET..UNDEFINED_OFFSET

private enum class LocationType {
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

sealed class LocationProvider {
    abstract fun getSourceLocation(element: IrElement, declaration: IrSymbol?, fileEntry: IrFileEntry?): SourceLocation
    abstract fun getSourceEndLocation(element: IrElement, declaration: IrSymbol?, fileEntry: IrFileEntry?): SourceLocation
    abstract fun nextLocation(element: IrElement, declaration: IrSymbol?, fileEntry: IrFileEntry?): SourceLocation
}

internal object LocationProviderStub : LocationProvider() {
    override fun getSourceLocation(element: IrElement, declaration: IrSymbol?, fileEntry: IrFileEntry?): SourceLocation =
        SourceLocation.NoLocation

    override fun getSourceEndLocation(element: IrElement, declaration: IrSymbol?, fileEntry: IrFileEntry?): SourceLocation =
        SourceLocation.NoLocation

    override fun nextLocation(element: IrElement, declaration: IrSymbol?, fileEntry: IrFileEntry?): SourceLocation =
        SourceLocation.NoLocation
}

internal object LocationProviderImpl : LocationProvider() {
    override fun getSourceLocation(element: IrElement, declaration: IrSymbol?, fileEntry: IrFileEntry?): SourceLocation =
        getSourceLocation(element, declaration, fileEntry, LocationType.START)

    override fun getSourceEndLocation(element: IrElement, declaration: IrSymbol?, fileEntry: IrFileEntry?): SourceLocation =
        getSourceLocation(element, declaration, fileEntry, LocationType.END)

    override fun nextLocation(element: IrElement, declaration: IrSymbol?, fileEntry: IrFileEntry?): SourceLocation =
        when (getSourceLocation(element, declaration, fileEntry)) {
            is SourceLocation.DefinedLocation -> SourceLocation.NextLocation
            else -> SourceLocation.NoLocation
        }

    private fun getSourceLocation(element: IrElement, declaration: IrSymbol?, fileEntry: IrFileEntry?, type: LocationType): SourceLocation {
        if (declaration.shouldIgnore) {
            return if (declaration is IrFunctionSymbol && declaration.owner.isInlinedCode)
                SourceLocation.NoLocation("Inlined function body")
            else SourceLocation.IgnoredLocation
        }

        if (fileEntry == null) return SourceLocation.NoLocation("fileEntry is null")
        if (element.hasSyntheticOrUndefinedLocation) return SourceLocation.NoLocation("Synthetic declaration")

        val path = fileEntry.name
        val (line, column) = type.getLineAndColumnNumberFor(element, fileEntry)

        return SourceLocation.DefinedLocation(
            path,
            line,
            column
        )
    }
}
