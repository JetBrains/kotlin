/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.wasm.declarations

import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.backend.js.checkers.*
import org.jetbrains.kotlin.ir.backend.js.wasm.WasmKlibExportingDeclaration

object WasmKlibExportsChecker {
    fun check(declarations: List<WasmKlibExportingDeclaration>, context: JsKlibDiagnosticContext, reporter: IrDiagnosticReporter) {

        val allExportedNameClashes = declarations.groupBy { it.exportingName }.filterValues { it.size > 1 }

        for (exportedDeclarationClashes in allExportedNameClashes.values) {
            for ((index, exportedDeclaration) in exportedDeclarationClashes.withIndex()) {
                val declaration = exportedDeclaration.declaration ?: continue
                val clashedWith = exportedDeclarationClashes.filterIndexed { i, _ -> i != index }

                val (sameExportType, differentExportType) = clashedWith.partition { it.exportKind == exportedDeclaration.exportKind }

                if (sameExportType.isNotEmpty()) {
                    reporter.at(declaration, context).report(
                        exportedDeclaration.exportKind.clashError,
                        exportedDeclaration.exportingName,
                        exportedDeclaration.render(),
                        sameExportType
                    )
                }

                if (differentExportType.isNotEmpty()) {
                    reporter.at(declaration, context).report(
                        exportedDeclaration.exportKind.crossClashError,
                        exportedDeclaration.exportingName,
                        exportedDeclaration.render(),
                        differentExportType
                    )
                }
            }
        }
    }
}