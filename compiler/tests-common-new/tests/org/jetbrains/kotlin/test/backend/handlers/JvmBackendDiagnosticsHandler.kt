/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticReporter
import org.jetbrains.kotlin.test.frontend.classic.handlers.withNewInferenceModeEnabled
import org.jetbrains.kotlin.test.frontend.fir.handlers.toMetaInfos
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.dependencyProvider
import org.jetbrains.kotlin.test.services.globalMetadataInfoHandler

class JvmBackendDiagnosticsHandler(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    private val reporter = ClassicDiagnosticReporter(testServices)

    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        val testFileToKtFileMap = getKtFiles(module)
        val ktFileToTestFileMap = testFileToKtFileMap.entries.associate { it.value to it.key }
        val generationState = info.classFileFactory.generationState
        val diagnostics = generationState.collectedExtraJvmDiagnostics.all()
        val configuration = reporter.createConfiguration(module)
        val withNewInferenceModeEnabled = testServices.withNewInferenceModeEnabled()
        for (diagnostic in diagnostics) {
            val ktFile = diagnostic.psiFile as? KtFile ?: continue
            val testFile = ktFileToTestFileMap[ktFile] ?: continue
            reporter.reportDiagnostic(diagnostic, module, testFile, configuration, withNewInferenceModeEnabled)
        }
        val ktDiagnosticReporter = generationState.diagnosticReporter as BaseDiagnosticsCollector
        val globalMetadataInfoHandler = testServices.globalMetadataInfoHandler
        for ((testFile, ktFile) in testFileToKtFileMap.entries) {
            val ktDiagnostics = ktDiagnosticReporter.diagnosticsByFilePath[ktFile.virtualFilePath] ?: continue
            ktDiagnostics.forEach {
                val metaInfos =
                    it.toMetaInfos(testFile, globalMetadataInfoHandler, false, false)
                globalMetadataInfoHandler.addMetadataInfosForFile(testFile, metaInfos)
            }
        }
    }

    private fun getKtFiles(module: TestModule): Map<TestFile, KtFile> {
        return when (module.frontendKind) {
            FrontendKinds.ClassicFrontend -> testServices.dependencyProvider.getArtifact(module, FrontendKinds.ClassicFrontend).ktFiles
            FrontendKinds.FIR -> testServices.dependencyProvider.getArtifact(module, FrontendKinds.FIR).firFiles.entries
                .associate { it.key to (it.value.psi as KtFile) }
            else -> testServices.assertions.fail { "Unknown frontend kind ${module.frontendKind}" }
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
