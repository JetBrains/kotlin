/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticCollectorService
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.ServiceRegistrationData
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.diagnosticsService
import org.jetbrains.kotlin.test.services.service

/**
 * Prevents the execution of box runners if the backend produced some errors.
 */
class NoBackendCompilationErrorsHandler(testServices: TestServices) : AnalysisHandler<BinaryArtifacts.Jvm>(
    testServices,
    // Must go on, because we may have multiple modules emitting IR diagnostics, and we want
    // to continue processing the next modules to collect everything for `GlobalMetadataInfoHandler`.
    failureDisablesNextSteps = false,
    doNotRunIfThereWerePreviousFailures = false,
) {
    override val artifactKind: TestArtifactKind<BinaryArtifacts.Jvm> get() = ArtifactKinds.Jvm

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::FirDiagnosticCollectorService))

    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        val ktDiagnosticReporter = info.classFileFactory.generationState.diagnosticReporter as BaseDiagnosticsCollector
        val diagnosticsService = testServices.diagnosticsService

        for ((_, ktDiagnostics) in ktDiagnosticReporter.diagnosticsByFilePath) {
            for (diagnostic in ktDiagnostics) {
                if (diagnostic.severity == Severity.ERROR) {
                    if (diagnosticsService.shouldRenderDiagnostic(module, diagnostic.factoryName, diagnostic.severity)) {
                        val diagnosticText = RootDiagnosticRendererFactory(diagnostic).render(diagnostic)
                        error("${diagnostic.factory.name}: $diagnosticText")
                    }
                }
            }
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
