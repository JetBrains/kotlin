/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalLibraryAbiReader::class)

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.backend.common.klibAbiVersionForManifest
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.ir.backend.js.serializeModuleIntoKlib
import org.jetbrains.kotlin.konan.library.impl.buildLibrary
import org.jetbrains.kotlin.library.KotlinIrSignatureVersion
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.abi.*
import org.jetbrains.kotlin.library.abi.AbiReadingFilter.*
import org.jetbrains.kotlin.test.backend.handlers.AbiDumpHandler.Companion.abiDumpFileExtension
import org.jetbrains.kotlin.test.backend.handlers.PreservedIrAbiDumpHandler.Companion.preservedAbiDumpDir
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.KlibAbiDumpDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.frontend.fir.getAllJsDependenciesPaths
import org.jetbrains.kotlin.test.frontend.fir.resolveLibraries
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifactHandler
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.nativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.test.utils.withExtension
import org.jetbrains.kotlin.util.klibMetadataVersionOrDefault
import java.io.File

abstract class AbiDumpHandler(testServices: TestServices) : BinaryArtifactHandler<BinaryArtifacts.KLib>(
    testServices,
    ArtifactKinds.KLib,
    failureDisablesNextSteps = true,
    doNotRunIfThereWerePreviousFailures = true,
) {
    protected val dumpers = hashMapOf<AbiSignatureVersion, MultiModuleInfoDumper>()

    abstract fun getAbiReadingFilters(module: TestModule): List<AbiReadingFilter>

    override fun processModule(module: TestModule, info: BinaryArtifacts.KLib) {
        val dumpMode = module.directives.singleOrZeroValue(KlibAbiDumpDirectives.DUMP_KLIB_ABI) ?: return

        val libraryAbi = LibraryAbiReader.readAbiInfo(
            info.outputFile,
            getAbiReadingFilters(module)
        )

        for (abiSignatureVersion in dumpMode.abiSignatureVersions) {
            val dumper = dumpers.getOrPut(abiSignatureVersion) { MultiModuleInfoDumper() }
            LibraryAbiRenderer.render(libraryAbi, dumper.builderForModule(module), AbiRenderingSettings(abiSignatureVersion))
        }
    }

    companion object {
        val DEFAULT_ABI_SIGNATURE_VERSION: KotlinIrSignatureVersion = KotlinIrSignatureVersion.V2

        fun abiDumpFileExtension(abiSignatureVersion: Int): String {
            val suffix = if (abiSignatureVersion == DEFAULT_ABI_SIGNATURE_VERSION.number) "" else "sig_v$abiSignatureVersion."
            return "${suffix}klib_abi.txt"
        }
    }
}

/**
 * Dumps KLIB ABI in the format of [LibraryAbiReader].
 *
 * Note: It's necessary to activate [KlibAbiDumpDirectives.DUMP_KLIB_ABI] directive and specify one of
 * [KlibAbiDumpDirectives.KlibAbiDumpMode]s to allow this handler dumping ABI.
 */
class KlibAbiDumpHandler(testServices: TestServices) : AbiDumpHandler(testServices) {
    override val directiveContainers get() = listOf(KlibAbiDumpDirectives)

    override fun getAbiReadingFilters(module: TestModule): List<AbiReadingFilter> = listOf(
        ExcludedPackages(module.directives[KlibAbiDumpDirectives.KLIB_ABI_DUMP_EXCLUDED_PACKAGES]),
        ExcludedClasses(module.directives[KlibAbiDumpDirectives.KLIB_ABI_DUMP_EXCLUDED_CLASSES]),
        NonPublicMarkerAnnotations(module.directives[KlibAbiDumpDirectives.KLIB_ABI_DUMP_NON_PUBLIC_MARKERS])
    )

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (dumpers.isEmpty()) return

        assertions.assertAll(
            dumpers.map { (abiSignatureVersion, dumper) ->
                val dumpFileExtension = abiDumpFileExtension(abiSignatureVersion.versionNumber)
                val lambda = {
                    val expectedFile = testServices
                        .moduleStructure
                        .originalTestDataFiles
                        .first()
                        .withExtension(dumpFileExtension)
                    assertions.assertEqualsToFile(expectedFile, dumper.generateResultingDump())
                }
                lambda
            }
        )
    }
}

class PreservedIrAbiDumpHandler(testServices: TestServices) : AbiDumpHandler(testServices) {
    // TODO: Change to IR_ABI_DUMP
    override val directiveContainers get() = listOf(KlibAbiDumpDirectives)

    override fun getAbiReadingFilters(module: TestModule) = emptyList<AbiReadingFilter>()

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (dumpers.isEmpty()) return

        assertions.assertAll(
            dumpers.map { (abiSignatureVersion, dumper) ->
                val dumpFileExtension = abiDumpFileExtension(abiSignatureVersion.versionNumber)
                val lambda = {
                    val expectedFile = testServices.preservedAbiDumpDir.resolve(dumpFileExtension)
                    assertions.assertEqualsToFile(expectedFile, dumper.generateResultingDump()) { abiDump ->
                        abiDump.replace(syntheticAccessorSignatureRegex, "\n")
                    }
                }
                lambda
            }
        )
    }

    companion object {
        val TestServices.preservedAbiDumpDir: File
            get() = temporaryDirectoryManager.getOrCreateTempDirectory("preserved_abi_dump")

        val syntheticAccessorSignatureRegex = """\n.*access\$.*\n""".toRegex()
    }
}

class IrAbiDumpPreservingHandler(
    testServices: TestServices,
) : AbstractIrHandler(
    testServices,
    failureDisablesNextSteps = true,
    doNotRunIfThereWerePreviousFailures = true,
) {
    // TODO: Change to IR_ABI_DUMP
    override val directiveContainers get() = listOf(KlibAbiDumpDirectives)

    private val dumpers = hashMapOf<AbiSignatureVersion, MultiModuleInfoDumper>()

    override fun processModule(module: TestModule, info: IrBackendInput) {
        val dumpMode = module.directives.singleOrZeroValue(KlibAbiDumpDirectives.DUMP_KLIB_ABI) ?: return

        val klibFile = serializeModule(module, info).outputFile
        val libraryAbi = LibraryAbiReader.readAbiInfo(klibFile)

        for (abiSignatureVersion in dumpMode.abiSignatureVersions) {
            val dumper = dumpers.getOrPut(abiSignatureVersion) { MultiModuleInfoDumper() }
            LibraryAbiRenderer.render(libraryAbi, dumper.builderForModule(module), AbiRenderingSettings(abiSignatureVersion))
        }
    }

    private fun serializeModule(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.KLib =
        when (inputArtifact) {
            is IrBackendInput.JsIrAfterFrontendBackendInput -> serializeJsModule(module, inputArtifact)
            is IrBackendInput.NativeAfterFrontendBackendInput -> serializeNativeModule(module, inputArtifact)
            else -> error("Unsupported backend input type: ${inputArtifact::class.simpleName}")
        }

    private fun serializeJsModule(module: TestModule, inputArtifact: IrBackendInput.JsIrAfterFrontendBackendInput): BinaryArtifacts.KLib {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val diagnosticReporter = DiagnosticReporterFactory.createReporter(configuration.messageCollector)
        val klibFile = JsEnvironmentConfigurator.getKlibArtifactFile(testServices, module.name)

        val libraries = resolveLibraries(configuration, getAllJsDependenciesPaths(module, testServices))

        serializeModuleIntoKlib(
            moduleName = configuration[CommonConfigurationKeys.MODULE_NAME]!!,
            configuration = configuration,
            diagnosticReporter = diagnosticReporter,
            metadataSerializer = inputArtifact.metadataSerializer,
            klibPath = klibFile.path,
            dependencies = libraries.map { it.library },
            moduleFragment = inputArtifact.irModuleFragment,
            irBuiltIns = inputArtifact.irPluginContext.irBuiltIns,
            cleanFiles = inputArtifact.icData,
            nopack = true,
            containsErrorCode = inputArtifact.hasErrors,
            jsOutputName = null
        )

        return BinaryArtifacts.KLib(klibFile, diagnosticReporter)
    }

    private fun serializeNativeModule(
        module: TestModule,
        inputArtifact: IrBackendInput.NativeAfterFrontendBackendInput,
    ): BinaryArtifacts.KLib {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val diagnosticReporter = DiagnosticReporterFactory.createReporter(configuration.messageCollector)
        val klibFile = NativeEnvironmentConfigurator.getKlibArtifactFile(testServices, module.name)

        val serializerOutput = org.jetbrains.kotlin.backend.common.serialization.serializeModuleIntoKlib(
            moduleName = inputArtifact.irModuleFragment.name.asString(),
            irModuleFragment = inputArtifact.irModuleFragment,
            configuration = configuration,
            diagnosticReporter = diagnosticReporter,
            cleanFiles = emptyList(),
            dependencies = inputArtifact.usedLibrariesForManifest,
            createModuleSerializer = { irDiagnosticReporter ->
                TODO("Can't import org.jetbrains.kotlin.backend.konan.serialization.KonanIrModuleSerializer")
            },
            metadataSerializer = inputArtifact.metadataSerializer ?: error("expected metadata serializer"),
        )

        buildLibrary(
            natives = emptyList(),
            included = emptyList(),
            linkDependencies = serializerOutput.neededLibraries,
            metadata = serializerOutput.serializedMetadata ?: testServices.assertions.fail { "expected serialized metadata" },
            ir = serializerOutput.serializedIr,
            versions = KotlinLibraryVersioning(
                compilerVersion = KotlinCompilerVersion.getVersion(),
                abiVersion = configuration.klibAbiVersionForManifest(),
                metadataVersion = configuration.klibMetadataVersionOrDefault()
            ),
            target = testServices.nativeEnvironmentConfigurator.getNativeTarget(module),
            output = klibFile.path,
            moduleName = configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME),
            nopack = true,
            shortName = null,
            manifestProperties = null,
        )

        return BinaryArtifacts.KLib(klibFile, diagnosticReporter)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (dumpers.isEmpty()) return

        for ((abiSignatureVersion, dumper) in dumpers) {
            val dumpFileExtension = abiDumpFileExtension(abiSignatureVersion.versionNumber)
            val abiDumpFile = testServices.preservedAbiDumpDir.resolve(dumpFileExtension)
            abiDumpFile.writeText(dumper.generateResultingDump())
        }
    }
}
