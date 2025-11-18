/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.BackendInputHandler
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

/**
 * Fails FIR2IR or Pre-serialization phases if there are some error diagnostics from, for example, IR actualization or IrInlineDeclarationChecker.
 */
class NoIrCompilationErrorsHandler(testServices: TestServices) : BackendInputHandler<IrBackendInput>(
    testServices,
    BackendKinds.IrBackend,
    // Must go on, because we may have multiple modules emitting IR diagnostics, and we want
    // to continue processing the next modules to collect everything for `GlobalMetadataInfoHandler`.
    // See: `compiler/testData/diagnostics/tests/multiplatform/topLevelFun/inlineFun.kt`
    failureDisablesNextSteps = true,
    doNotRunIfThereWerePreviousFailures = false,
) {
    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::DiagnosticsService))

    override fun processModule(module: TestModule, info: IrBackendInput) {
        val diagnosticsByFilePath = info.diagnosticReporter.diagnosticsByFilePath
        val diagnosticsService = testServices.diagnosticsService

        for ((file, diagnostics) in diagnosticsByFilePath) {
            for (diagnostic in diagnostics) {
                if (
                    diagnostic.severity == Severity.ERROR &&
                    diagnosticsService.shouldRenderDiagnostic(module, diagnostic.factoryName, diagnostic.severity)
                ) {
                    val severity = diagnostic.severity.toCompilerMessageSeverity().toString().toLowerCaseAsciiOnly()
                    val message = diagnostic.renderMessage()
                    error("/$file:${diagnostic.firstRange}: $severity: $message")
                }
            }
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
