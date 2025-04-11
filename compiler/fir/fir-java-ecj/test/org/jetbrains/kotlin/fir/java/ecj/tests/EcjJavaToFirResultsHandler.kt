/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.ecj.tests

import org.jetbrains.kotlin.codeMetaInfo.model.CodeMetaInfo
import org.jetbrains.kotlin.codeMetaInfo.renderConfigurations.AbstractCodeMetaInfoRenderConfiguration
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.backend.handlers.assertFileDoesntExist
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.impl.testConfiguration
import org.jetbrains.kotlin.test.model.BinaryArtifactHandler
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.globalMetadataInfoHandler
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import java.io.File

/**
 * Handler for results from the ECJ-to-FIR conversion process.
 */
class EcjJavaToFirResultsHandler(
    testServices: TestServices,
    failureDisablesNextSteps: Boolean = false,
    doNotRunIfThereWerePreviousFailures: Boolean = false
) : BinaryArtifactHandler<EcjJavaToFirCompilationArtifact>(testServices, EcjJavaToFirCompilationArtifact.Kind, failureDisablesNextSteps, doNotRunIfThereWerePreviousFailures) {

    private val dumper: MultiModuleInfoDumper = MultiModuleInfoDumper()
    private val globalMetadataInfoHandler = testServices.globalMetadataInfoHandler

    override fun processModule(module: TestModule, info: EcjJavaToFirCompilationArtifact) {
        val file = module.files.single()

        // If there are diagnostics, add them to the metadata info handler
        if (info.diagnostics.isNotEmpty()) {
            globalMetadataInfoHandler.addMetadataInfosForFile(
                file,
                info.diagnostics.map { EcjJavaToFirDiagnosticCodeMetaInfo(it, info.javaSource) }
            )
        }

        // If the FIR Java class is null, report an error
        if (info.firJavaClass == null) {
            assertions.fail { "Failed to convert Java class to FIR: ${info.diagnostics.joinToString("\n")}" }
        }
        val builderForModule = dumper.builderForModule(module)
        val renderer = FirRenderer(
            builder = builderForModule,
        )

        renderer.renderElementAsString(info.firJavaClass)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val expectedFile = expectedFile()

        if (dumper.isEmpty()) {
            assertions.assertFileDoesntExist(expectedFile, FirDiagnosticsDirectives.FIR_DUMP)
        } else {
            val actualText = dumper.generateResultingDump()
            assertions.assertEqualsToFile(expectedFile, actualText, message = { "Content is not equal" })
        }
    }

    private fun expectedFile(): File {
        // TODO: change according to multiple testdata files
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        val extension = ".fir.txt"
        val originalExpectedFilePath = testDataFile.parentFile.resolve("${testDataFile.nameWithoutFirExtension}$extension").path

        @OptIn(TestInfrastructureInternals::class)
        val expectedFilePath = testServices.testConfiguration
            .metaTestConfigurators
            .fold(originalExpectedFilePath) { fileName, configurator ->
                configurator.transformTestDataPath(fileName)
            }

        return File(expectedFilePath)
    }

    protected val File.nameWithoutFirExtension: String
        get() = nameWithoutExtension.removeSuffix(".fir")
}

/**
 * Code metadata info for ECJ-to-FIR conversion diagnostics.
 */
internal class EcjJavaToFirDiagnosticCodeMetaInfo(
    private val diagnostic: String,
    text: String
) : CodeMetaInfo {
    override var start = 0
    override var end = 0

    override val tag: String = "ERROR"
    override val renderConfiguration = RenderConfiguration()
    override val attributes: MutableList<String> = mutableListOf()

    override fun asString(): String = renderConfiguration.asString(this)

    class RenderConfiguration : AbstractCodeMetaInfoRenderConfiguration() {
        override fun asString(codeMetaInfo: CodeMetaInfo): String = 
            (codeMetaInfo as EcjJavaToFirDiagnosticCodeMetaInfo).diagnostic
    }
}
