/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalLibraryAbiReader::class)

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.library.abi.AbiReadingFilter.SyntheticAccessors
import org.jetbrains.kotlin.library.abi.AbiRenderingSettings
import org.jetbrains.kotlin.library.abi.AbiSignatureVersion.Companion.resolveByVersionNumber
import org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader
import org.jetbrains.kotlin.library.abi.LibraryAbiReader
import org.jetbrains.kotlin.library.abi.LibraryAbiRenderer
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.backend.AbstractKlibSerializerFacade
import org.jetbrains.kotlin.test.backend.handlers.KlibAbiDumpBeforeInliningSavingHandler.Companion.abiDumpBeforeInlining
import org.jetbrains.kotlin.test.backend.handlers.KlibAbiDumpHandler.Companion.DEFAULT_ABI_SIGNATURE_VERSION
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.KlibAbiConsistencyDirectives
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifactHandler
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.KlibBasedEnvironmentConfiguratorUtils
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import java.io.File

class KlibAbiDumpBeforeInliningSavingHandler(
    testServices: TestServices,
    private val serializerFacade: Constructor<AbstractKlibSerializerFacade>,
) : AbstractIrHandler(
    testServices,
    failureDisablesNextSteps = true,
    doNotRunIfThereWerePreviousFailures = true,
) {
    override val directiveContainers get() = listOf(KlibAbiConsistencyDirectives)

    private val dumper = MultiModuleInfoDumper()

    override fun processModule(module: TestModule, info: IrBackendInput) {
        if (KlibAbiConsistencyDirectives.CHECK_SAME_ABI_AFTER_INLINING !in module.directives) return

        val klibFile = serializeModule(module, info).outputFile
        val libraryAbi = LibraryAbiReader.readAbiInfo(klibFile, SyntheticAccessors())

        LibraryAbiRenderer.render(
            libraryAbi,
            dumper.builderForModule(module),
            AbiRenderingSettings(resolveByVersionNumber(DEFAULT_ABI_SIGNATURE_VERSION.number))
        )
    }

    private fun serializeModule(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.KLib {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val diagnosticReporter = DiagnosticReporterFactory.createReporter(configuration.messageCollector)
        val outputFile = getAbiCheckKlibArtifactFile(testServices, module.name)

        serializerFacade.invoke(testServices).serializeBare(module, inputArtifact, outputFile, configuration, diagnosticReporter)

        return BinaryArtifacts.KLib(outputFile, diagnosticReporter)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (dumper.isEmpty()) return
        testServices.abiDumpBeforeInlining.writeText(dumper.generateResultingDump())
    }

    companion object : KlibBasedEnvironmentConfiguratorUtils {
        val TestServices.abiDumpBeforeInlining: File
            get() = temporaryDirectoryManager.rootDir.resolve("abi_dump_before_inlining.txt")

        fun getAbiCheckKlibArtifactFile(testServices: TestServices, moduleName: String) =
            getKlibArtifactFile(testServices, "${moduleName}AbiCheck")
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
        if (KlibAbiConsistencyDirectives.CHECK_SAME_ABI_AFTER_INLINING !in module.directives) return

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
