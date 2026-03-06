/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.checks

import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.backend.js.checkers.JsKlibDiagnosticContext
import org.jetbrains.kotlin.ir.backend.js.wasm.collectAllExportNamesSequence
import org.jetbrains.kotlin.ir.backend.js.wasm.declarations.WasmKlibExportsChecker
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

internal fun checkCrossModuleExportClashes(
    allModules: List<IrModuleFragment>,
    configuration: CompilerConfiguration,
) {
    val allDeclarations = allModules.asSequence().flatMap { module ->
        module.collectAllExportNamesSequence()
    }.toList()

    val diagnosticsCollector = DiagnosticsCollectorImpl()
    val diagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(
        diagnosticsCollector,
        configuration.languageVersionSettings
    )
    val diagnosticContext = JsKlibDiagnosticContext(configuration)
    WasmKlibExportsChecker.check(allDeclarations, diagnosticContext, diagnosticReporter)

    if (diagnosticsCollector.hasErrors) {
        val messageCollector = configuration.messageCollector
        for ((_, diagnostics) in diagnosticsCollector.diagnosticsByFilePath) {
            for (diagnostic in diagnostics) {
                messageCollector.report(
                    CompilerMessageSeverity.ERROR,
                    diagnostic.renderMessage(),
                )
            }
        }
        throw CompilationErrorException()
    }
}
