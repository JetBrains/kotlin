/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticReporter
import org.jetbrains.kotlin.test.frontend.classic.handlers.withNewInferenceModeEnabled
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.junit.jupiter.api.fail

class JvmBackendDiagnosticsHandler(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    private val reporter = ClassicDiagnosticReporter(testServices)

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::DiagnosticsService))

    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        reportDiagnostics(module, info)
        reportKtDiagnostics(module, info)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        checkFullDiagnosticRender()
    }

    private fun reportDiagnostics(module: TestModule, info: BinaryArtifacts.Jvm) {
        val testFiles = module.files.associateBy { "/${it.name}" }
        val configuration = reporter.createConfiguration(module)
        val withNewInferenceModeEnabled = testServices.withNewInferenceModeEnabled()

        val diagnostics = info.classFileFactory.generationState.collectedExtraJvmDiagnostics.all()
        for (diagnostic in diagnostics) {
            val ktFile = diagnostic.psiFile as? KtFile ?: fail("PSI file is not a KtFile: ${diagnostic.psiFile}")
            val testFile = testFiles[ktFile.virtualFilePath] ?: fail("Test file for KtFile not found: ${ktFile.virtualFilePath}")
            reporter.reportDiagnostic(diagnostic, module, testFile, configuration, withNewInferenceModeEnabled)
        }
    }

    private fun reportKtDiagnostics(module: TestModule, info: BinaryArtifacts.Jvm) {
        val ktDiagnosticReporter = info.classFileFactory.generationState.diagnosticReporter as BaseDiagnosticsCollector
        reportKtDiagnostics(module, ktDiagnosticReporter)
    }
}
