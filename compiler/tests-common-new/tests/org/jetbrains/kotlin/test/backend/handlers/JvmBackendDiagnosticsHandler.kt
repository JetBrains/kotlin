/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.PsiDiagnosticUtils
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.diagnostics.KtDefaultJvmErrorMessages
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
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
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.io.File

class JvmBackendDiagnosticsHandler(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    private val reporter = ClassicDiagnosticReporter(testServices)

    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        reportDiagnostics(module, info)
        reportKtDiagnostics(module, info)
        checkFullDiagnosticRender(module, info)
    }

    private fun getKtFiles(module: TestModule): Map<TestFile, KtFile> {
        return when (module.frontendKind) {
            FrontendKinds.ClassicFrontend -> testServices.dependencyProvider.getArtifact(module, FrontendKinds.ClassicFrontend).ktFiles
            FrontendKinds.FIR -> testServices.dependencyProvider.getArtifact(module, FrontendKinds.FIR).mainFirFiles.entries
                .associate { it.key to (it.value.psi as KtFile) }
            else -> testServices.assertions.fail { "Unknown frontend kind ${module.frontendKind}" }
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}

    private fun getLineAndColumnRange(diagnostic: KtDiagnostic): PsiDiagnosticUtils.LineAndColumnRange {
        val file = diagnostic.psiElement.containingFile
        val textRanges = diagnostic.textRanges
        return DiagnosticUtils.getLineAndColumnRange(file, textRanges)
    }

    private fun reportDiagnostics(module: TestModule, info: BinaryArtifacts.Jvm) {
        val testFileToKtFileMap = getKtFiles(module)
        val ktFileToTestFileMap = testFileToKtFileMap.entries.associate { it.value to it.key }
        val generationState = info.classFileFactory.generationState
        val configuration = reporter.createConfiguration(module)
        val withNewInferenceModeEnabled = testServices.withNewInferenceModeEnabled()

        val diagnostics = generationState.collectedExtraJvmDiagnostics.all()
        for (diagnostic in diagnostics) {
            val ktFile = diagnostic.psiFile as? KtFile ?: continue
            val testFile = ktFileToTestFileMap[ktFile] ?: continue
            reporter.reportDiagnostic(diagnostic, module, testFile, configuration, withNewInferenceModeEnabled)
        }
    }

    private fun reportKtDiagnostics(module: TestModule, info: BinaryArtifacts.Jvm) {
        val testFileToKtFileMap = getKtFiles(module)
        val generationState = info.classFileFactory.generationState

        val ktDiagnosticReporter = generationState.diagnosticReporter as BaseDiagnosticsCollector
        val globalMetadataInfoHandler = testServices.globalMetadataInfoHandler
        for ((testFile, ktFile) in testFileToKtFileMap.entries) {
            val ktDiagnostics = ktDiagnosticReporter.diagnosticsByFilePath[ktFile.virtualFilePath] ?: continue
            ktDiagnostics.forEach {
                val metaInfos =
                    it.toMetaInfos(module, testFile, globalMetadataInfoHandler, false, false)
                globalMetadataInfoHandler.addMetadataInfosForFile(testFile, metaInfos)
            }
        }
    }

    private fun checkFullDiagnosticRender(module: TestModule, info: BinaryArtifacts.Jvm) {
        if (DiagnosticsDirectives.RENDER_DIAGNOSTICS_FULL_TEXT !in module.directives) return

        val testFileToKtFileMap = getKtFiles(module)
        val generationState = info.classFileFactory.generationState

        val ktDiagnosticReporter = generationState.diagnosticReporter as BaseDiagnosticsCollector
        val reportedDiagnostics = mutableListOf<String>()
        for (ktFile in testFileToKtFileMap.values) {
            val ktDiagnostics = ktDiagnosticReporter.diagnosticsByFilePath[ktFile.virtualFilePath] ?: continue
            ktDiagnostics.forEach {
                val severity = AnalyzerWithCompilerReport.convertSeverity(it.severity).toString().toLowerCaseAsciiOnly()
                val message = KtDefaultJvmErrorMessages.MAP[it.factory]?.render(it)
                val position = getLineAndColumnRange(it).start
                reportedDiagnostics += "/${ktFile.name}:${position.line}:${position.column}: $severity: $message"
            }
        }

        if (reportedDiagnostics.isNotEmpty()) {
            testServices.assertions.assertEqualsToFile(
                File(FileUtil.getNameWithoutExtension(testFileToKtFileMap.keys.first().originalFile.absolutePath) + ".diag.txt"),
                reportedDiagnostics.joinToString(separator = "\n\n")
            )
        }
    }
}
