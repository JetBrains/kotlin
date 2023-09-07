/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.checkers.declarations

import org.jetbrains.kotlin.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.backend.js.checkers.JsKlibExportingDeclaration
import org.jetbrains.kotlin.ir.backend.js.checkers.JsKlibExportedDeclarationsChecker
import org.jetbrains.kotlin.ir.backend.js.checkers.JsKlibErrors

object JsKlibEsModuleExportsChecker : JsKlibExportedDeclarationsChecker {
    override fun check(
        declarations: List<JsKlibExportingDeclaration>,
        reporter: KtDiagnosticReporterWithImplicitIrBasedContext,
    ) {
        val allExportedNameClashes = declarations.groupBy { it.exportingName }.filterValues { it.size > 1 }

        for (exportedDeclarationClashes in allExportedNameClashes.values) {
            for ((index, exportedDeclaration) in exportedDeclarationClashes.withIndex()) {
                val declaration = exportedDeclaration.declaration ?: continue
                val clashedWith = exportedDeclarationClashes.filterIndexed { i, _ -> i != index }
                reporter.at(declaration).report(JsKlibErrors.EXPORTING_JS_NAME_CLASH_ES, exportedDeclaration.exportingName, clashedWith)
            }
        }
    }
}
