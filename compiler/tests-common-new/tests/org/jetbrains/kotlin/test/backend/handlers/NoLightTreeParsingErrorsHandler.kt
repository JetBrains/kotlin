/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirAnalysisHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticCollectorService
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.fir.lightTreeSyntaxDiagnosticsReporterHolder
import org.jetbrains.kotlin.test.services.ServiceRegistrationData
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.diagnosticsService
import org.jetbrains.kotlin.test.services.service

class NoLightTreeParsingErrorsHandler(testServices: TestServices) : FirAnalysisHandler(testServices, failureDisablesNextSteps = false) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::FirDiagnosticCollectorService))

    override fun processModule(module: TestModule, info: FirOutputArtifact) {
        for (part in info.partsForDependsOnModules) {
            val diagnosticsPerFile = testServices.lightTreeSyntaxDiagnosticsReporterHolder?.reporter?.diagnosticsByFilePath ?: continue
            val diagnosticsService = testServices.diagnosticsService

            for ((_, ktDiagnostics) in diagnosticsPerFile.entries) {
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
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
