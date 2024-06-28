/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.checkers.declarations

import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.backend.js.checkers.*

object JsKlibEsModuleExportsChecker : JsKlibExportedDeclarationsChecker {
    override fun check(
        declarations: List<JsKlibExportingDeclaration>,
        context: JsKlibDiagnosticContext,
        reporter: IrDiagnosticReporter
    ) {
        val allExportedNameClashes = declarations.groupBy { it.exportingName }.filterValues { it.size > 1 }

        for (exportedDeclarationClashes in allExportedNameClashes.values) {
            for ((index, exportedDeclaration) in exportedDeclarationClashes.withIndex()) {
                val declaration = exportedDeclaration.declaration ?: continue
                val clashedWith = exportedDeclarationClashes.filterIndexed { i, _ -> i != index }
                reporter.at(declaration, context).report(
                    JsKlibErrors.EXPORTING_JS_NAME_CLASH_ES,
                    exportedDeclaration.exportingName,
                    clashedWith
                )
            }
        }
    }
}
