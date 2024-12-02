/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.LineAndColumn
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.wasm.ir.WasmExpressionBuilder
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation
import java.util.IdentityHashMap

val IrElement.hasSyntheticOrUndefinedLocation: Boolean
    get() = startOffset in SYNTHETIC_OFFSET..UNDEFINED_OFFSET ||
            endOffset in SYNTHETIC_OFFSET..UNDEFINED_OFFSET

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

private val debugFriendlyOrigins = IdentityHashMap<IrDeclarationOrigin, Boolean>().apply {
    set(IrDeclarationOrigin.DEFINED, true)
    set(IrDeclarationOrigin.LOCAL_FUNCTION, true)
    set(IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA, true)
    set(IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER, true)
}

private val IrSymbol?.shouldIgnore: Boolean
    get() = this?.let {
        val owner = it.owner as? IrFunction ?: return@let false
        owner.getPackageFragment().packageFqName.startsWith(StandardClassIds.BASE_KOTLIN_PACKAGE) ||
                owner.origin !in debugFriendlyOrigins
    } == true

fun IrElement.getSourceLocation(
    declaration: IrSymbol?,
    fileEntry: IrFileEntry?,
    type: LocationType = LocationType.START
): SourceLocation {
    val isIgnoredDeclaration = declaration.shouldIgnore

    if (fileEntry == null) return SourceLocation.NoLocation("fileEntry is null")
    if (isIgnoredDeclaration && declaration is IrFunctionSymbol && (declaration.owner.isInline)) return SourceLocation.NoLocation("Inlined function body")

    val path = fileEntry.name
    var (line, column) = type.getLineAndColumnNumberFor(this, fileEntry)

    if (line < 0 || column < 0) {
        if (!isIgnoredDeclaration) return SourceLocation.NoLocation("startLine or startColumn < 0")
        line = 0
        column = 0
    }
    return if (isIgnoredDeclaration) {
        SourceLocation.IgnoredLocation(
            path,
            line,
            column
        )
    } else {
        SourceLocation.Location(
            path,
            line,
            column
        )
    }
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
