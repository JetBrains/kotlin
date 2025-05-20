/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalLibraryAbiReader::class)

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.cli.pipeline.web.JsFir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebKlibSerializationPipelinePhase
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.js.config.outputDir
import org.jetbrains.kotlin.js.config.outputName
import org.jetbrains.kotlin.js.config.produceKlibFile
import org.jetbrains.kotlin.library.abi.AbiReadingFilter.SyntheticAccessors
import org.jetbrains.kotlin.library.abi.AbiRenderingSettings
import org.jetbrains.kotlin.library.abi.AbiSignatureVersion.Companion.resolveByVersionNumber
import org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader
import org.jetbrains.kotlin.library.abi.LibraryAbiReader
import org.jetbrains.kotlin.library.abi.LibraryAbiRenderer
import org.jetbrains.kotlin.test.backend.handlers.KlibAbiDumpHandler.Companion.DEFAULT_ABI_SIGNATURE_VERSION
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.KlibAbiConsistencyDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliBasedOutputArtifact
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifactHandler
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import java.io.File

private val TestServices.abiDumpBeforeInlining: File
    get() = temporaryDirectoryManager.rootDir.resolve("abi_dump_before_inlining.txt")

private fun shouldCheckAbiConsistency(module: TestModule): Boolean =
    KlibAbiConsistencyDirectives.CHECK_SAME_ABI_AFTER_INLINING in module.directives &&
            "+${LanguageFeature.IrInlinerBeforeKlibSerialization.name}" in module.directives[LANGUAGE]

abstract class AbstractKlibAbiDumpBeforeInliningSavingHandler(
    testServices: TestServices,
) : AbstractIrHandler(
    testServices = testServices,
    failureDisablesNextSteps = true,
    doNotRunIfThereWerePreviousFailures = true,
) {
    override val directiveContainers get() = listOf(KlibAbiConsistencyDirectives)

    private val dumper = MultiModuleInfoDumper()

    override fun processModule(module: TestModule, info: IrBackendInput) {
        if (!shouldCheckAbiConsistency(module)) return

        val klibFile = serializeModule(module, info).outputFile
        val libraryAbi = LibraryAbiReader.readAbiInfo(klibFile)

        LibraryAbiRenderer.render(
            libraryAbi,
            dumper.builderForModule(module),
            AbiRenderingSettings(resolveByVersionNumber(DEFAULT_ABI_SIGNATURE_VERSION.number))
        )
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (dumper.isEmpty()) return
        testServices.abiDumpBeforeInlining.writeText(dumper.generateResultingDump())
    }

    protected fun getAbiCheckKlibArtifactFile(moduleName: String) =
        testServices.temporaryDirectoryManager.getOrCreateTempDirectory("klibsForAbiCheck").resolve("$moduleName.klib")

    protected abstract fun serializeModule(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.KLib
}

class FirJsKlibAbiDumpBeforeInliningSavingHandler(testServices: TestServices) :
    AbstractKlibAbiDumpBeforeInliningSavingHandler(testServices) {
    override fun serializeModule(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.KLib {
        require(inputArtifact is Fir2IrCliBasedOutputArtifact<*>) {
            "FirKlibSerializerCliWebFacade expects Fir2IrCliBasedWebOutputArtifact as input"
        }
        val cliArtifact = inputArtifact.cliArtifact
        require(cliArtifact is JsFir2IrPipelineArtifact) {
            "FirKlibSerializerCliWebFacade expects JsFir2IrPipelineArtifact as input"
        }

        val tmpConfiguration = cliArtifact.configuration.copy()

        val messageCollector = tmpConfiguration.messageCollector
        val diagnosticReporter = DiagnosticReporterFactory.createPendingReporter(messageCollector)
        val outputFile = getAbiCheckKlibArtifactFile(module.name)

        tmpConfiguration.produceKlibFile = true
        tmpConfiguration.outputDir = outputFile.parentFile
        tmpConfiguration.outputName = outputFile.name.removeSuffix(".klib")

        val input = cliArtifact.copy(diagnosticCollector = diagnosticReporter, configuration = tmpConfiguration)

        WebKlibSerializationPipelinePhase.executePhase(input)

        return BinaryArtifacts.KLib(outputFile, diagnosticReporter)
    }
}

class KlibAbiDumpAfterInliningVerifyingHandler(testServices: TestServices) : BinaryArtifactHandler<BinaryArtifacts.KLib>(
    testServices,
    ArtifactKinds.KLib,
    failureDisablesNextSteps = true,
    doNotRunIfThereWerePreviousFailures = true,
) {
    private val dumper = MultiModuleInfoDumper()

    override val directiveContainers get() = listOf(KlibAbiConsistencyDirectives)

    override fun processModule(module: TestModule, info: BinaryArtifacts.KLib) {
        if (!shouldCheckAbiConsistency(module)) return

        val libraryAbi = LibraryAbiReader.readAbiInfo(info.outputFile, SyntheticAccessors())

        LibraryAbiRenderer.render(
            libraryAbi,
            dumper.builderForModule(module),
            AbiRenderingSettings(resolveByVersionNumber(DEFAULT_ABI_SIGNATURE_VERSION.number))
        )
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (dumper.isEmpty()) return
        val expectedFile = testServices.abiDumpBeforeInlining
        assertions.assertEqualsToFile(expectedFile, dumper.generateResultingDump())
    }
}
