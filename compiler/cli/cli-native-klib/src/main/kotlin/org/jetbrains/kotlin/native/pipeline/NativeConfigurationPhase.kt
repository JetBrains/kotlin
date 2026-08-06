/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.pipeline

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageDiagnostics
import org.jetbrains.kotlin.backend.common.linkage.partial.setupPartialLinkageConfig
import org.jetbrains.kotlin.backend.konan.NativeBackendDiagnostics
import org.jetbrains.kotlin.cli.CliDiagnostics.KONAN_ARGUMENT_ERROR
import org.jetbrains.kotlin.cli.CliDiagnostics.KONAN_ARGUMENT_STRONG_WARNING
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.common.arguments.isNativeSecondStage
import org.jetbrains.kotlin.cli.common.checkForUnexpectedKlibLibraries
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.common.createPhaseConfig
import org.jetbrains.kotlin.cli.common.prohibitExportKlibToOlderAbiVersionAtSecondStage
import org.jetbrains.kotlin.cli.common.setupCommonKlibArguments
import org.jetbrains.kotlin.cli.diagnosticFactoriesStorage
import org.jetbrains.kotlin.cli.pipeline.*
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.ir.inline.diagnostics.IrInlinerErrors
import org.jetbrains.kotlin.konan.config.*
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.isCache
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.File

/**
 * Configuration phase for native klib compilation pipeline.
 * Sets up the compiler configuration from K2NativeKlibCompilerArguments.
 */
object NativeConfigurationPhase : AbstractConfigurationPhase<K2NativeCompilerArguments>(
    name = "NativeConfigurationPhase",
    preActions = setOf(PerformanceNotifications.InitializationStarted),
    postActions = setOf(PerformanceNotifications.InitializationFinished, CheckCompilationErrors.CheckDiagnosticCollector),
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
    ) = fillConfiguration(input.arguments, input.rootDisposable, configuration)

    fun fillConfiguration(
        arguments: K2NativeCompilerArguments,
        rootDisposable: Disposable,
        configuration: CompilerConfiguration,
    ) {
        configuration.diagnosticFactoriesStorage?.registerDiagnosticContainers(
            PartialLinkageDiagnostics,
            IrInlinerErrors,
            NativeBackendDiagnostics
        )

        configuration.setupCommonKlibArguments(arguments, canBeMetadataKlibCompilation = !arguments.isNativeSecondStage(), rootDisposable)
        configuration.setupSources(arguments)
        val outputKind = configuration.setupNativeBasicSettings(arguments)
        configuration.setupLibraries(arguments, outputKind)
        configuration.setupMisc(arguments)
        configuration.setupPartialLinkageConfig(arguments, KONAN_ARGUMENT_ERROR)

        if (arguments.isNativeSecondStage()) {
            configuration.phaseConfig = createPhaseConfig(arguments)
            configuration.prohibitExportKlibToOlderAbiVersionAtSecondStage()
        }
    }

    private fun CompilerConfiguration.setupSources(arguments: K2NativeCompilerArguments) {
        val commonSources = arguments.commonSources.toSet().map { File(it).absoluteFile.normalize() }
        val hmppModuleStructure = this[CommonConfigurationKeys.HMPP_MODULE_STRUCTURE]
        arguments.freeArgs.forEach { path ->
            val normalizedPath = File(path).absoluteFile.normalize()
            addKotlinSourceRoot(path, normalizedPath in commonSources, hmppModuleStructure?.getModuleNameForSource(path))
        }
    }

    private fun CompilerConfiguration.setupNativeBasicSettings(arguments: K2NativeCompilerArguments): CompilerOutputKind {
        val outputKind = CompilerOutputKind.valueOf((arguments.produce ?: "program").uppercase())
        konanProducedArtifactKind = outputKind

        arguments.kotlinHome?.let { konanHome = it }
        arguments.konanDataDir?.let { konanDataDir = it }

        arguments.moduleName?.let { moduleName = it }
        parseShortModuleName(arguments, outputKind)?.let { konanShortModuleName = it }

        arguments.target?.let { konanTarget = it }
        targetPlatform = konanTarget?.let { NativePlatforms.nativePlatformByTargetNames(listOf(it)) }
            ?: NativePlatforms.unspecifiedNativePlatform

        arguments.manifestNativeTargets.takeIf { it.isNotEmpty() }?.let {
            konanManifestNativeTargets = parseManifestNativeTargets(it)
        }

        arguments.outputName?.let { konanOutputPath = it }

        // We need to download dependencies only if we use them ( = there are files to compile).
        checkDependencies = kotlinSourceRoots.isNotEmpty() ||
                arguments.includes.isNotEmpty() ||
                arguments.exportedLibraries.isNotEmpty() ||
                (outputKind == CompilerOutputKind.PROGRAM && arguments.libraries.isNotEmpty()) ||
                outputKind.isCache ||
                arguments.checkDependencies

        konanRefinesModules = arguments.refinesPaths.filterNot(String::isEmpty)

        return outputKind
    }

    private fun CompilerConfiguration.setupLibraries(arguments: K2NativeCompilerArguments, outputKind: CompilerOutputKind) {
        konanLibraries = arguments.libraries.toList()

        arguments.friendModules?.let {
            konanFriendLibraries = it.split(File.pathSeparator).filterNot(String::isEmpty)
            checkForUnexpectedKlibLibraries(
                librariesToCheck = konanFriendLibraries,
                librariesToCheckArgument = K2NativeCompilerArguments::friendModules.cliArgument,
                allLibraries = konanLibraries,
                allLibrariesArgument = K2NativeCompilerArguments::libraries.cliArgument
            )
        }

        exportedLibraries = parseSelectedLibraries(arguments, outputKind)
        checkForUnexpectedKlibLibraries(
            librariesToCheck = exportedLibraries,
            librariesToCheckArgument = K2NativeCompilerArguments::exportedLibraries.cliArgument,
            allLibraries = konanLibraries,
            allLibrariesArgument = K2NativeCompilerArguments::libraries.cliArgument
        )

        konanIncludedLibraries = parseIncludedLibraries(arguments, outputKind)

        parseLibraryToAddToCache(arguments, outputKind)?.let { konanLibraryToAddToCache = it }

        konanLibraries = buildList {
            this += konanLibraries
            this += konanIncludedLibraries
            addIfNotNull(konanLibraryToAddToCache)
        }

        konanNoDefaultLibs = arguments.nodefaultlibs || !konanLibraryToAddToCache.isNullOrEmpty()
        konanNoStdlib = arguments.nostdlib || !konanLibraryToAddToCache.isNullOrEmpty()
        konanPurgeUserLibs = arguments.purgeUserLibs

        konanDontCompressKlib = arguments.nopack
        arguments.manifestFile?.let { konanManifestAddend = it }

        konanIncludedBinaries = arguments.includeBinaries.toList()
        konanNativeLibraries = arguments.nativeLibraries.toList()

        arguments.headerKlibPath?.let { konanGeneratedHeaderKlibPath = it }

        arguments.writeDependenciesOfProducedKlibTo?.let { konanWriteDependenciesOfProducedKlibTo = it }
    }

    private fun CompilerConfiguration.setupMisc(arguments: K2NativeCompilerArguments) {
        konanPrintIr = arguments.printIr
        konanPrintFiles = arguments.printFiles
        konanPrintBitcode = arguments.printBitCode

        arguments.verifyCompiler?.let { verifyCompiler = it == "true" }
        verifyBitcode = arguments.verifyBitCode
    }

    // TODO: Support short names for current module in ObjC export and lift this limitation.
    private fun CompilerConfiguration.parseShortModuleName(
        arguments: K2NativeCompilerArguments,
        outputKind: CompilerOutputKind
    ): String? {
        val input = arguments.shortModuleName ?: return null

        return if (outputKind != CompilerOutputKind.LIBRARY) {
            report(
                KONAN_ARGUMENT_STRONG_WARNING,
                "${K2NativeCompilerArguments::shortModuleName.cliArgument} CLI argument is only supported when producing a Kotlin library, " +
                        "but the compiler is producing ${outputKind.name.lowercase()}"
            )
            null
        } else input
    }

    private fun CompilerConfiguration.parseManifestNativeTargets(targetStrings: Array<String>): List<KonanTarget> {
        val trimmedTargetStrings = targetStrings.map { it.trim() }
        val [recognizedTargetNames, unrecognizedTargetNames] = trimmedTargetStrings.partition {
            it in KonanTarget.predefinedTargets.keys
        }

        if (unrecognizedTargetNames.isNotEmpty()) {
            report(
                KONAN_ARGUMENT_STRONG_WARNING,
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

    private fun CompilerConfiguration.parseSelectedLibraries(
        arguments: K2NativeCompilerArguments,
        outputKind: CompilerOutputKind,
    ): List<String> {
        val exportedLibraries = arguments.exportedLibraries.toList()

        return if (exportedLibraries.isNotEmpty() &&
            outputKind != CompilerOutputKind.FRAMEWORK &&
            outputKind != CompilerOutputKind.STATIC &&
            outputKind != CompilerOutputKind.DYNAMIC
        ) {
            report(
                KONAN_ARGUMENT_STRONG_WARNING,
                "${K2NativeCompilerArguments::exportedLibraries.cliArgument} CLI argument is only supported when producing frameworks or native libraries, " +
                        "but the compiler is producing ${outputKind.name.lowercase()}"
            )

            emptyList()
        } else {
            exportedLibraries
        }
    }

    private fun CompilerConfiguration.parseIncludedLibraries(
        arguments: K2NativeCompilerArguments,
        outputKind: CompilerOutputKind
    ): List<String> {
        val includes = arguments.includes.toList()

        return if (includes.isNotEmpty() && outputKind == CompilerOutputKind.LIBRARY) {
            report(
                KONAN_ARGUMENT_ERROR,
                "${K2NativeCompilerArguments::includes.cliArgument} CLI argument is not supported when producing ${outputKind.name.lowercase()}"
            )
            emptyList()
        } else {
            includes
        }
    }

    private fun CompilerConfiguration.parseLibraryToAddToCache(
        arguments: K2NativeCompilerArguments,
        outputKind: CompilerOutputKind,
    ): String? {
        val input = arguments.libraryToAddToCache ?: return null

        return if (!outputKind.isCache) {
            report(
                KONAN_ARGUMENT_ERROR,
                "${K2NativeCompilerArguments::libraryToAddToCache.cliArgument} can't be used when not producing cache"
            )
            null
        } else if (!arguments.outputName.isNullOrEmpty()) {
            report(
                KONAN_ARGUMENT_ERROR,
                "${K2NativeCompilerArguments::libraryToAddToCache.cliArgument} already implicitly sets output file name"
            )
            null
        } else input
    }
}
