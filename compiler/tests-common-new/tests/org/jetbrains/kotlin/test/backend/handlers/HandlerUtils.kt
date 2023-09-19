/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.resolve.jvm.diagnostics.KtDefaultJvmErrorMessages
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticCodeMetaInfo
import org.jetbrains.kotlin.test.frontend.fir.handlers.toMetaInfos
import org.jetbrains.kotlin.test.model.BinaryArtifactHandler
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.io.File

inline fun IrBackendInput.processAllIrModuleFragments(
    module: TestModule,
    processor: (irModuleFragment: IrModuleFragment, moduleName: String) -> Unit
) {
    dependentIrModuleFragments.forEach { processor(it, it.name.asString()) }
    processor(irModuleFragment, module.name)
}

fun BinaryArtifactHandler<*>.reportKtDiagnostics(module: TestModule, ktDiagnosticReporter: BaseDiagnosticsCollector) {
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

fun BinaryArtifactHandler<*>.checkFullDiagnosticRender(module: TestModule) {
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
                    val message = RootDiagnosticRendererFactory(it).render(it)
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
