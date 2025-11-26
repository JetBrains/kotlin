/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.pipeline

import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.cli.common.arguments.K2NativeKlibCompilerArguments
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.setupCommonKlibArguments
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.pipeline.AbstractConfigurationPhase
import org.jetbrains.kotlin.cli.pipeline.ArgumentsPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.ConfigurationUpdater
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.native.NativeKlibLibrariesResolveSupport
import org.jetbrains.kotlin.native.StandaloneNativeKlibCompilationConfig
import org.jetbrains.kotlin.native.StandalonePhaseContext
import org.jetbrains.kotlin.utils.KotlinNativePaths

/**
 * Configuration phase for Native klib compilation.
 *
 * This phase extends the standard configuration phase and produces a
 * [NativeKlibConfigurationArtifact] containing the [StandalonePhaseContext]
 * needed for subsequent compilation phases.
 */
object NativeKlibConfigurationPhase : AbstractConfigurationPhase<K2NativeKlibCompilerArguments>(
    name = "NativeKlibConfigurationPhase",
    postActions = setOf(CheckCompilationErrors.CheckMessageCollector),
    configurationUpdaters = listOf(NativeKlibConfigurationUpdater)
) {
    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion = MetadataVersion(*versionArray)
}

/**
 * Second phase that takes the standard [ConfigurationPipelineArtifact] and creates
 * the Native-specific [NativeKlibConfigurationArtifact] with the [StandalonePhaseContext].
 */
object NativeKlibContextCreationPhase : PipelinePhase<ConfigurationPipelineArtifact, NativeKlibConfigurationArtifact>(
    name = "NativeKlibContextCreationPhase",
) {
    override fun executePhase(input: ConfigurationPipelineArtifact): NativeKlibConfigurationArtifact? {
        val configuration = input.configuration
        val messageCollector = configuration.messageCollector

        // Determine target
        val konanHome = configuration.get(KonanConfigKeys.KONAN_DATA_DIR) ?: KotlinNativePaths.homePath.absolutePath
        val distribution = Distribution(konanHome)
        val platformManager = PlatformManager(distribution)
        val targetName = configuration.get(KonanConfigKeys.TARGET)
        val target = targetName?.let {
            platformManager.targetManager(it).target
        } ?: platformManager.hostPlatform.target

        // Resolve libraries
        val resolveSupport = NativeKlibLibrariesResolveSupport(
            configuration,
            target,
            distribution,
            resolveManifestDependenciesLenient = true
        )

        // Create environment
        @OptIn(org.jetbrains.kotlin.K1Deprecation::class)
        val environment = KotlinCoreEnvironment.createForProduction(
            input.rootDisposable,
            configuration,
            EnvironmentConfigFiles.NATIVE_CONFIG_FILES
        )

        // Create config
        val config = StandaloneNativeKlibCompilationConfig(
            project = environment.project,
            configuration = configuration,
            target = target,
            resolvedLibraries = resolveSupport.resolvedLibraries,
        )

        // Create phase context
        val phaseContext = StandalonePhaseContext(config)

        return NativeKlibConfigurationArtifact(
            phaseContext = phaseContext,
            environment = environment,
            configuration = configuration,
            diagnosticCollector = input.diagnosticCollector,
        )
    }
}

/**
 * Configuration updater for Native klib compilation.
 */
object NativeKlibConfigurationUpdater : ConfigurationUpdater<K2NativeKlibCompilerArguments>() {
    override fun fillConfiguration(
        input: ArgumentsPipelineArtifact<K2NativeKlibCompilerArguments>,
        configuration: CompilerConfiguration,
    ) {
        val arguments = input.arguments
        val messageCollector = configuration.messageCollector

        // Setup common klib arguments
        configuration.setupCommonKlibArguments(arguments, canBeMetadataKlibCompilation = true, input.rootDisposable)

        // Add source roots
        configuration.addKotlinSourceRoots(arguments.freeArgs)

        // Configure native-specific options
        configureNativeOptions(configuration, arguments, messageCollector)
    }

    private fun configureNativeOptions(
        configuration: CompilerConfiguration,
        arguments: K2NativeKlibCompilerArguments,
        messageCollector: org.jetbrains.kotlin.cli.common.messages.MessageCollector,
    ) {
        // Target
        arguments.target?.let { configuration.put(KonanConfigKeys.TARGET, it) }

        // Konan data dir
        arguments.konanDataDir?.let { configuration.put(KonanConfigKeys.KONAN_DATA_DIR, it) }

        // Output
        arguments.outputName?.let { configuration.put(KonanConfigKeys.OUTPUT, it) }

        // Module name
        arguments.moduleName?.let { configuration.put(KonanConfigKeys.MODULE_NAME, it) }

        // Produce (always library for klib compilation)
        configuration.put(KonanConfigKeys.PRODUCE, CompilerOutputKind.LIBRARY)

        // Libraries
        arguments.libraries?.let { libs ->
            configuration.put(KonanConfigKeys.LIBRARY_FILES, libs.toList())
        }

        // Friend modules
        arguments.friendModules?.let { friends ->
            configuration.put(KonanConfigKeys.FRIEND_MODULES, friends.split(",").filter { it.isNotEmpty() })
        }

        // Refines paths
        arguments.refinesPaths?.let { refines ->
            configuration.put(KonanConfigKeys.REFINES_MODULES, refines.toList())
        }

        // No stdlib
        configuration.put(KonanConfigKeys.NOSTDLIB, arguments.nostdlib)

        // No default libs
        configuration.put(KonanConfigKeys.NODEFAULTLIBS, arguments.nodefaultlibs)

        // No pack
        configuration.put(KonanConfigKeys.NOPACK, arguments.nopack)

        // Include binaries
        arguments.includeBinaries?.let { bins ->
            configuration.put(KonanConfigKeys.INCLUDED_BINARY_FILES, bins.toList())
        }

        // Native libraries
        arguments.nativeLibraries?.let { nativeLibs ->
            configuration.put(KonanConfigKeys.NATIVE_LIBRARY_FILES, nativeLibs.toList())
        }

        // Manifest file
        arguments.manifestFile?.let { manifest ->
            configuration.put(KonanConfigKeys.MANIFEST_FILE, manifest)
        }

        // Short module name
        arguments.shortModuleName?.let { shortName ->
            configuration.put(KonanConfigKeys.SHORT_MODULE_NAME, shortName)
        }

        // Header klib path
        arguments.headerKlibPath?.let { headerPath ->
            configuration.put(KonanConfigKeys.HEADER_KLIB, headerPath)
        }

        // Write dependencies
        arguments.writeDependenciesOfProducedKlibTo?.let { deps ->
            configuration.put(KonanConfigKeys.WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO, deps)
        }

        // Manifest native targets
        arguments.manifestNativeTargets?.let { targets ->
            val konanHome = arguments.konanDataDir ?: KotlinNativePaths.homePath.absolutePath
            val platformManager = PlatformManager(Distribution(konanHome))
            val konanTargets = targets.mapNotNull { targetString ->
                try {
                    platformManager.targetManager(targetString).target
                } catch (e: Exception) {
                    messageCollector.report(CompilerMessageSeverity.WARNING, "Unknown native target: $targetString")
                    null
                }
            }
            configuration.put(KonanConfigKeys.MANIFEST_NATIVE_TARGETS, konanTargets)
        }

        // Fake override validator
        configuration.put(KonanConfigKeys.FAKE_OVERRIDE_VALIDATOR, arguments.fakeOverrideValidator)

        // Export KDoc
        configuration.put(KonanConfigKeys.EXPORT_KDOC, arguments.exportKDoc)
    }
}
