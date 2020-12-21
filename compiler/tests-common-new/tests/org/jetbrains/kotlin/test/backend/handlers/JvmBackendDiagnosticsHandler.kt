/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticReporter
import org.jetbrains.kotlin.test.frontend.classic.handlers.withNewInferenceModeEnabled
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.dependencyProvider

class JvmBackendDiagnosticsHandler(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    private val reporter = ClassicDiagnosticReporter(testServices)

    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        val testFileToKtFileMap = testServices.dependencyProvider.getArtifact(module, FrontendKinds.ClassicFrontend).ktFiles
        val ktFileToTestFileMap = testFileToKtFileMap.entries.map { it.value to it.key }.toMap()
        val generationState = info.classFileFactory.generationState
        val diagnostics = generationState.collectedExtraJvmDiagnostics.all()
        val configuration = reporter.createConfiguration(module)
        val withNewInferenceModeEnabled = testServices.withNewInferenceModeEnabled()
        for (diagnostic in diagnostics) {
            val ktFile = diagnostic.psiFile as? KtFile ?: continue
            val testFile = ktFileToTestFileMap[ktFile] ?: continue
            reporter.reportDiagnostic(diagnostic, module, testFile, configuration, withNewInferenceModeEnabled)
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
