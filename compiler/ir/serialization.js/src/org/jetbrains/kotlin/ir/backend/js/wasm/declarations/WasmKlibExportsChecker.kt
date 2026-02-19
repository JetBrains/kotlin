/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.wasm.declarations

import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.backend.js.checkers.*
import org.jetbrains.kotlin.ir.backend.js.wasm.ExportKind
import org.jetbrains.kotlin.ir.backend.js.wasm.WasmKlibErrors
import org.jetbrains.kotlin.ir.backend.js.wasm.WasmKlibExportingDeclaration

object WasmKlibExportsChecker {
    fun check(declarations: List<WasmKlibExportingDeclaration>, context: JsKlibDiagnosticContext, reporter: IrDiagnosticReporter) {

        val allExportedNameClashes = declarations.groupBy { it.exportingName }.filterValues { it.size > 1 }

        for (exportedDeclarationClashes in allExportedNameClashes.values) {
            for ((index, exportedDeclaration) in exportedDeclarationClashes.withIndex()) {
                val declaration = exportedDeclaration.declaration ?: continue
                val clashedWith = exportedDeclarationClashes.filterIndexed { i, _ -> i != index }

                val clashedWithSameExportType = clashedWith.filter { it.exportKind == exportedDeclaration.exportKind }
                val clashedWithOtherExportType = clashedWith.filter { it.exportKind != exportedDeclaration.exportKind }

                val errorSameExportType = if (exportedDeclaration.exportKind == ExportKind.JsExport) {
                    WasmKlibErrors.EXPORTING_JS_NAME_CLASH
                } else {
                    WasmKlibErrors.WASM_EXPORT_CLASH
                }

                if (clashedWithSameExportType.isNotEmpty()) {
                    reporter.at(declaration, context).report(
                        errorSameExportType,
                        exportedDeclaration.exportingName,
                        clashedWithSameExportType
                    )
                }

                if (clashedWithOtherExportType.isNotEmpty()) {
                    reporter.at(declaration, context).report(
                        WasmKlibErrors.EXPORTING_JS_NAME_WASM_EXPORT_CLASH,
                        exportedDeclaration.exportingName,
                        clashedWithOtherExportType
                    )
                }
            }
        }
    }
}