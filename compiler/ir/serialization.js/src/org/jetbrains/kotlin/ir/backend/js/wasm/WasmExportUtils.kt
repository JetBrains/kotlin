/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.wasm

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory3
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.exportedJsExportName
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.isJsExportDeclaration
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.isJsExportIgnoreDeclaration
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isExpect
import org.jetbrains.kotlin.name.JsStandardClassIds
import org.jetbrains.kotlin.name.WasmStandardClassIds
import kotlin.sequences.filter

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

fun IrModuleFragment.collectWasmExportNamesList(): List<WasmKlibExportingDeclaration> = buildList {
    for (irFile in files) {
        for (declaration in irFile.declarations) {
            // expect declarations are eliminated during wasmLowerings - export declarations not generated
            // KT-86267 K/Wasm: prohibit placing JsExport/WasmExport on expect declarations
            if (declaration is IrDeclarationWithName &&
                declaration.isWasmExportDeclaration() &&
                !declaration.isEffectivelyExternal() &&
                !declaration.isExpect
            ) {
                add(WasmKlibExportingDeclaration(declaration.getWasmExportName(), irFile, declaration, ExportKind.WasmExport))
            }
        }
    }
}

fun IrModuleFragment.collectJsExportNamesList(): List<WasmKlibExportingDeclaration> = buildList {
    for (irFile in files) {
        val isFileJsExported = irFile.annotations.hasAnnotation(
            JsStandardClassIds.Annotations.JsExport.asSingleFqName()
        )
        for (declaration in irFile.declarations) {
            // expect declarations are eliminated during wasmLowerings - export declarations not generated
            // KT-86267 K/Wasm: prohibit placing JsExport/WasmExport on expect declarations
            if (declaration is IrDeclarationWithName &&
                !declaration.isEffectivelyExternal() &&
                !declaration.isExpect &&
                (isFileJsExported && !declaration.isJsExportIgnoreDeclaration() || declaration.isJsExportDeclaration())
            ) {
                add(WasmKlibExportingDeclaration(declaration.exportedJsExportName, irFile, declaration, ExportKind.JsExport))
            }
        }
    }
}

fun IrModuleFragment.collectAllExportNames(): List<WasmKlibExportingDeclaration> =
    collectWasmExportNamesList() + collectJsExportNamesList()
