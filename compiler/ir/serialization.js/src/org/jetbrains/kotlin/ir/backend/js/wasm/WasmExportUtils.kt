/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.wasm

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory3
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.collectJsExportNamesSequence
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.name.WasmStandardClassIds

enum class ExportKind(
    val clashError: KtDiagnosticFactory3<String, String, List<WasmKlibExportingDeclaration>>,
    val crossClashError: KtDiagnosticFactory3<String, String, List<WasmKlibExportingDeclaration>>
) {
    JsExport(
        WasmKlibErrors.EXPORTING_JS_NAME_CLASH,
        WasmKlibErrors.EXPORTING_JS_NAME_WASM_EXPORT_CLASH
    ),
    WasmExport(
        WasmKlibErrors.WASM_EXPORT_CLASH,
        WasmKlibErrors.WASM_EXPORT_EXPORTING_JS_NAME_CLASH
    )
}

fun IrAnnotationContainer.isWasmExportDeclaration(): Boolean {
    return hasAnnotation(WasmStandardClassIds.Annotations.WasmExport.asSingleFqName())
}

fun IrDeclarationWithName.getWasmExportName(): String {
    val annotation = getAnnotation(WasmStandardClassIds.Annotations.WasmExport.asSingleFqName())!!
    val nameFromAnnotation = (annotation.arguments[0] as? IrConst)?.value as? String
    return nameFromAnnotation ?: name.identifier
}

fun IrModuleFragment.collectWasmExportNamesSequence(): Sequence<Triple<IrFile, IrDeclarationWithName, String>> =
    files.asSequence().flatMap { irFile ->
        irFile.declarations.asSequence()
            .filterIsInstance<IrDeclarationWithName>()
            .filter { it.isWasmExportDeclaration() && !it.isEffectivelyExternal() }
            .map { declaration ->
                Triple(irFile, declaration, declaration.getWasmExportName())
            }
    }

fun IrModuleFragment.collectAllExportNamesSequence(): Sequence<WasmKlibExportingDeclaration> = sequence {
    collectWasmExportNamesSequence().forEach { (file, decl, name) ->
        yield(WasmKlibExportingDeclaration(name, file, decl, ExportKind.WasmExport))
    }

    collectJsExportNamesSequence().forEach { (file, decl, name) ->
        yield(WasmKlibExportingDeclaration(name, file, decl, ExportKind.JsExport))
    }
}