/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.cli.common.fir.SequentialPositionFinder
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.codeMetaInfo.model.DiagnosticCodeMetaInfo
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.diagnostics.KtDefaultJvmErrorMessages
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticReporter
import org.jetbrains.kotlin.test.frontend.classic.handlers.withNewInferenceModeEnabled
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticCodeMetaInfo
import org.jetbrains.kotlin.test.frontend.fir.handlers.toMetaInfos
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.junit.jupiter.api.fail
import java.io.File

class JvmBackendDiagnosticsHandler(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    private val reporter = ClassicDiagnosticReporter(testServices)

    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        reportDiagnostics(module, info)
        reportKtDiagnostics(module, info)
        checkFullDiagnosticRender(module)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}

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
        val globalMetadataInfoHandler = testServices.globalMetadataInfoHandler
        val firParser = module.directives.singleOrZeroValue(FirDiagnosticsDirectives.FIR_PARSER)
        val lightTreeComparingModeEnabled = firParser != null && FirDiagnosticsDirectives.COMPARE_WITH_LIGHT_TREE in module.directives
        val lightTreeEnabled = firParser == FirParser.LightTree

        val processedModules = mutableSetOf<TestModule>()

        fun processModule(module: TestModule) {
            if (!processedModules.add(module)) return
            for (testFile in module.files) {
                val ktDiagnostics = ktDiagnosticReporter.diagnosticsByFilePath["/${testFile.name}"] ?: continue
                ktDiagnostics.forEach {
                    val metaInfos = it.toMetaInfos(module, testFile, globalMetadataInfoHandler, lightTreeEnabled, lightTreeComparingModeEnabled)
                    globalMetadataInfoHandler.addMetadataInfosForFile(testFile, metaInfos)
                }
            }
            for ((moduleName, _, _) in module.dependsOnDependencies) {
                val dependantModule = testServices.dependencyProvider.getTestModule(moduleName)
                processModule(dependantModule)
            }
        }

        processModule(module)
    }

    private fun checkFullDiagnosticRender(module: TestModule) {
        if (DiagnosticsDirectives.RENDER_ALL_DIAGNOSTICS_FULL_TEXT !in module.directives) return

        val reportedDiagnostics = mutableListOf<String>()
        for (testFile in module.files) {
            val finder =
                SequentialPositionFinder(testServices.sourceFileProvider.getContentOfSourceFile(testFile).byteInputStream().reader())
            for (metaInfo in testServices.globalMetadataInfoHandler.getReportedMetaInfosForFile(testFile).sortedBy { it.start }) {
                when (metaInfo) {
                    is DiagnosticCodeMetaInfo -> metaInfo.diagnostic.let {
                        val message = DefaultErrorMessages.render(it)
                        val position = DiagnosticUtils.getLineAndColumnRange(it.psiFile, it.textRanges).start
                        reportedDiagnostics +=
                            renderDiagnosticMessage(it.psiFile.name, it.severity, message, position.line, position.column)
                    }
                    is FirDiagnosticCodeMetaInfo -> metaInfo.diagnostic.let {
                        val message = KtDefaultJvmErrorMessages.MAP[it.factory]?.render(it)
                        val position = finder.findNextPosition(DiagnosticUtils.firstRange(it.textRanges).startOffset, false)
                        reportedDiagnostics +=
                            renderDiagnosticMessage(testFile.relativePath, it.severity, message, position.line, position.column)
                    }
                }
            }
        }

        testServices.assertions.assertEqualsToFile(
            File(FileUtil.getNameWithoutExtension(module.files.first().originalFile.absolutePath) + ".diag.txt"),
            reportedDiagnostics.joinToString(separator = "\n\n", postfix = "\n")
        )
    }

    private fun renderDiagnosticMessage(fileName: String, severity: Severity, message: String?, line: Int, column: Int): String {
        val severityString = AnalyzerWithCompilerReport.convertSeverity(severity).toString().toLowerCaseAsciiOnly()
        return "/${fileName}:$line:$column: $severityString: $message"
    }
}
