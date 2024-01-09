/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.diagnostics.PsiDiagnosticUtils
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_FIR_DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirAnalysisHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticCollectorService
import org.jetbrains.kotlin.test.frontend.fir.handlers.firDiagnosticCollectorService
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.ServiceRegistrationData
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.service

class NoFirCompilationErrorsHandler(testServices: TestServices) : FirAnalysisHandler(testServices, failureDisablesNextSteps = true) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::FirDiagnosticCollectorService))

    override fun processModule(module: TestModule, info: FirOutputArtifact) {
        for (part in info.partsForDependsOnModules) {
            var hasError = false

            val ignoreErrors = IGNORE_FIR_DIAGNOSTICS in part.module.directives

            val diagnosticsPerFile = testServices.firDiagnosticCollectorService.getFrontendDiagnosticsForModule(info)
            for ((firFile, diagnostics) in diagnosticsPerFile) {
                for (diagnostic in diagnostics) {
                    if (diagnostic.severity == Severity.ERROR) {
                        hasError = true
                        if (!ignoreErrors) {
                            val diagnosticText = RootDiagnosticRendererFactory(diagnostic).render(diagnostic)
                            val range = diagnostic.textRanges.first()
                            val locationText = firFile.source?.psi?.containingFile?.let { psiFile ->
                                PsiDiagnosticUtils.atLocation(psiFile, range)
                            } ?: "${firFile.name}:$range"
                            error("${diagnostic.factory.name}: $diagnosticText at $locationText")
                        }
                    }
                }
            }

            if (!hasError && ignoreErrors) {
                assertions.fail { "Test contains $IGNORE_FIR_DIAGNOSTICS directive but no errors was reported. Please remove directive" }
            }
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
