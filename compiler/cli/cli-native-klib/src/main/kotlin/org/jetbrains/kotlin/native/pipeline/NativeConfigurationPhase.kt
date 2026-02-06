/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.pipeline

import org.jetbrains.kotlin.backend.common.linkage.partial.setupPartialLinkageConfig
import org.jetbrains.kotlin.cli.CliDiagnostics.KONAN_ARGUMENT_ERROR
import org.jetbrains.kotlin.cli.CliDiagnostics.KONAN_ARGUMENT_WARNING
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.common.checkForUnexpectedKlibLibraries
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.createPhaseConfig
import org.jetbrains.kotlin.cli.common.setupCommonKlibArguments
import org.jetbrains.kotlin.cli.pipeline.*
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.js.config.fakeOverrideValidator
import org.jetbrains.kotlin.konan.config.*
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.platform.konan.NativePlatforms

/**
 * Configuration phase for native klib compilation pipeline.
 * Sets up the compiler configuration from K2NativeKlibCompilerArguments.
 */
object NativeConfigurationPhase : AbstractConfigurationPhase<K2NativeCompilerArguments>(
    name = "NativeConfigurationPhase",
    preActions = setOf(PerformanceNotifications.InitializationStarted),
    postActions = setOf(CheckCompilationErrors.CheckDiagnosticCollector),
    configurationUpdaters = listOf(NativeKlibConfigurationUpdater)
) {
    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion {
        return MetadataVersion(*versionArray)
    }
}

/**
 * Configuration updater that fills the CompilerConfiguration from K2NativeKlibCompilerArguments.
 */
object NativeKlibConfigurationUpdater : ConfigurationUpdater<K2NativeCompilerArguments>() {
    override fun fillConfiguration(
        input: ArgumentsPipelineArtifact<K2NativeCompilerArguments>,
        configuration: CompilerConfiguration,
    ) {
        val arguments = input.arguments
        val rootDisposable = input.rootDisposable
        configuration.setupCommonKlibArguments(arguments, canBeMetadataKlibCompilation = true, rootDisposable)
        val phaseConfig = createPhaseConfig(arguments)
        configuration.phaseConfig = phaseConfig
        setupNativeKlibConfiguration(configuration, arguments)
    }

    private fun setupNativeKlibConfiguration(
        configuration: CompilerConfiguration,
        arguments: K2NativeCompilerArguments,
    ) {
        val commonSources = arguments.commonSources?.toSet().orEmpty().map { java.io.File(it).absoluteFile.normalize() }
        val hmppModuleStructure = configuration.get(CommonConfigurationKeys.HMPP_MODULE_STRUCTURE)
        arguments.freeArgs.forEach { path ->
            val normalizedPath = java.io.File(path).absoluteFile.normalize()
            configuration.addKotlinSourceRoot(path, normalizedPath in commonSources, hmppModuleStructure?.getModuleNameForSource(path))
        }

        configuration.konanProducedArtifactKind = CompilerOutputKind.LIBRARY
        arguments.moduleName?.let { configuration.moduleName = it }
        arguments.kotlinHome?.let { configuration.konanHome = it }
        arguments.target?.let { configuration.konanTarget = it }
        configuration.targetPlatform = configuration.konanTarget?.let {
            NativePlatforms.nativePlatformByTargetNames(listOf(it))
        } ?: NativePlatforms.unspecifiedNativePlatform

        configuration.konanLibraries = arguments.libraries?.toList().orEmpty()
        arguments.friendModules?.let {
            configuration.konanFriendLibraries = it.split(File.pathSeparator).filterNot(String::isEmpty)

            configuration.checkForUnexpectedKlibLibraries(
                librariesToCheck = configuration.konanFriendLibraries,
                librariesToCheckArgument = K2NativeCompilerArguments::friendModules.cliArgument,
                allLibraries = configuration.konanLibraries,
                allLibrariesArgument = K2NativeCompilerArguments::libraries.cliArgument
            )
        }

        configuration.konanNoStdlib = arguments.nostdlib
        configuration.konanNoDefaultLibs = arguments.nodefaultlibs
        configuration.konanPurgeUserLibs = arguments.purgeUserLibs

        @Suppress("DEPRECATION")
        configuration.konanNoEndorsedLibs = arguments.noendorsedlibs
        configuration.konanDontCompressKlib = arguments.nopack

        arguments.outputName?.let { configuration.konanOutputPath = it }
        arguments.refinesPaths?.let {
            configuration.konanRefinesModules = it.filterNot(String::isEmpty)
        }

        configuration.konanIncludedBinaries = arguments.includeBinaries?.toList().orEmpty()

        arguments.manifestFile?.let { configuration.konanManifestAddend = it }
        arguments.headerKlibPath?.let { configuration.konanGeneratedHeaderKlibPath = it }
        arguments.shortModuleName?.let { configuration.konanShortModuleName = it }
        arguments.includes?.let {
            configuration.konanIncludedLibraries = it.toList()
        }

        configuration.konanPrintIr = arguments.printIr
        configuration.konanPrintFiles = arguments.printFiles
        arguments.verifyCompiler?.let {
            configuration.verifyCompiler = it == "true"
        }
        configuration.fakeOverrideValidator = arguments.fakeOverrideValidator
        arguments.konanDataDir?.let { configuration.konanDataDir = it }

        configuration.checkDependencies = arguments.checkDependencies
        arguments.writeDependenciesOfProducedKlibTo?.let {
            configuration.konanWriteDependenciesOfProducedKlibTo = it
        }
        arguments.manifestNativeTargets?.let {
            configuration.konanManifestNativeTargets = parseManifestNativeTargets(it, configuration)
        }

        configuration.konanExportKdoc = arguments.exportKDoc

        configuration.setupPartialLinkageConfig(
            mode = arguments.partialLinkageMode,
            logLevel = arguments.partialLinkageLogLevel,
            compilerModeAllowsUsingPartialLinkage = false, // Don't run PL when producing KLIB
            onWarning = { configuration.report(KONAN_ARGUMENT_WARNING, it) },
            onError = { configuration.report(KONAN_ARGUMENT_ERROR, it) }
        )
    }

    private fun parseManifestNativeTargets(
        targetStrings: Array<String>,
        configuration: CompilerConfiguration,
    ): List<KonanTarget> {
        val trimmedTargetStrings = targetStrings.map { it.trim() }
        val (recognizedTargetNames, unrecognizedTargetNames) = trimmedTargetStrings.partition {
            it in KonanTarget.predefinedTargets.keys
        }

        if (unrecognizedTargetNames.isNotEmpty()) {
            configuration.report(
                KONAN_ARGUMENT_WARNING,
                """
                    The following target names passed to the -Xmanifest-native-targets are not recognized:
                    ${unrecognizedTargetNames.joinToString(separator = ", ")}

                    List of known target names:
                    ${KonanTarget.predefinedTargets.keys.joinToString(separator = ", ")}
                """.trimIndent()
            )
        }

        return recognizedTargetNames.map { KonanTarget.predefinedTargets[it]!! }
    }
}
