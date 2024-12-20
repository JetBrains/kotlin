/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.LineAndColumn
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.lower.CallableReferenceLowering
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.AbstractSuspendFunctionsLowering
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.wasm.ir.WasmExpressionBuilder
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation
import java.util.IdentityHashMap

private val IrElement.hasSyntheticOrUndefinedLocation: Boolean
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
    set(IrDeclarationOrigin.LOWERED_SUSPEND_FUNCTION, true)
    set(AbstractSuspendFunctionsLowering.DECLARATION_ORIGIN_COROUTINE_IMPL_INVOKE, true)
}

private val IrDeclaration.isInlinedCode: Boolean
    get() = this is IrFunction && (isInline || origin == IrDeclarationOrigin.INLINE_LAMBDA)

private val IrDeclaration.isStdlibDeclaration: Boolean
    get() = getPackageFragment().packageFqName.startsWith(StandardClassIds.BASE_KOTLIN_PACKAGE)

private val IrDeclaration.isArtificialDeclarationOfLambdaImpl: Boolean
    get() = parentClassOrNull?.origin == CallableReferenceLowering.LAMBDA_IMPL &&
            origin != IrDeclarationOrigin.DEFINED &&
            origin != AbstractSuspendFunctionsLowering.DECLARATION_ORIGIN_COROUTINE_IMPL_INVOKE

private val IrSymbol?.shouldIgnore: Boolean
    get() {
        val owner = this?.owner as? IrDeclaration ?: return false
        return owner.isStdlibDeclaration || owner.isArtificialDeclarationOfLambdaImpl || owner.origin !in debugFriendlyOrigins
    }

fun IrElement.getSourceLocation(
    declaration: IrSymbol?,
    file: IrFile?,
    type: LocationType = LocationType.START
): SourceLocation {
    if (declaration.shouldIgnore) {
        return if (declaration is IrFunctionSymbol && declaration.owner.isInlinedCode)
            SourceLocation.NoLocation("Inlined function body")
        else SourceLocation.IgnoredLocation
    }

    val fileEntry = file?.fileEntry

    if (fileEntry == null) return SourceLocation.NoLocation("fileEntry is null")
    if (hasSyntheticOrUndefinedLocation) return SourceLocation.NoLocation("Synthetic declaration")

    val path = fileEntry.name
    val (line, column) = type.getLineAndColumnNumberFor(this, fileEntry)

    return SourceLocation.DefinedLocation(
        // TODO Drop "file" usages after KT-58406 fix and replace IrFile with IrFileEntry
        file.module.name.asString(),
        path,
        line,
        column
    )
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
