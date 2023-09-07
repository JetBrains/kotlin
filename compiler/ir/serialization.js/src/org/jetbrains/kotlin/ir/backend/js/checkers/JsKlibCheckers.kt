/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.checkers

import org.jetbrains.kotlin.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.ir.backend.js.checkers.declarations.JsKlibEsModuleExportsChecker
import org.jetbrains.kotlin.ir.backend.js.checkers.declarations.JsKlibOtherModuleExportsChecker
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.library.SerializedIrFile

object JsKlibCheckers {
    private val exportedDeclarationsCheckers = listOf(
        JsKlibEsModuleExportsChecker,
        JsKlibOtherModuleExportsChecker
    )

    fun check(
        cleanFiles: List<SerializedIrFile>,
        dirtyFiles: List<IrFile>,
        exportedNames: Map<IrFile, Map<IrDeclarationWithName, String>>,
        diagnosticReporter: DiagnosticReporter,
        configuration: CompilerConfiguration
    ) {
        val reporter = KtDiagnosticReporterWithImplicitIrBasedContext(diagnosticReporter, configuration.languageVersionSettings)
        val exportedDeclarations = JsKlibExportingDeclaration.collectDeclarations(cleanFiles, dirtyFiles, exportedNames)
        for (checker in exportedDeclarationsCheckers) {
            checker.check(exportedDeclarations, reporter)
        }
    }
}
