/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.kotlin.K1Deprecation::class)

package org.jetbrains.kotlin.cli.pipeline.native

import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.cli.common.messages.getLogger
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.DuplicatedUniqueNameStrategy
import org.jetbrains.kotlin.config.KlibConfigurationKeys
import org.jetbrains.kotlin.config.useLightTree
import org.jetbrains.kotlin.config.zipFileSystemAccessor
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.RequiredUnresolvedLibrary
import org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.library.toUnresolvedLibraries
import org.jetbrains.kotlin.native.FirOutput
import org.jetbrains.kotlin.native.firFrontendWithLightTree
import org.jetbrains.kotlin.native.firFrontendWithPsi

object NativeFrontendPipelinePhase : PipelinePhase<ConfigurationPipelineArtifact, NativeFrontendPipelineArtifact>(
    name = "NativeFrontendPipelinePhase",
    postActions = setOf(PerformanceNotifications.AnalysisFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: ConfigurationPipelineArtifact): NativeFrontendPipelineArtifact? {
        val configuration = input.configuration
        val environment = KotlinCoreEnvironment.createForProduction(
            input.rootDisposable,
            configuration,
            EnvironmentConfigFiles.NATIVE_CONFIG_FILES
        )

        // Create phase context for native klib compilation
        val nativeConfig = createNativeKlibConfig(input, environment)
        val phaseContext = NativeKlibPhaseContext(
            config = nativeConfig,
            disposable = input.rootDisposable,
            messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY),
            performanceManager = configuration.get(CommonConfigurationKeys.PERF_MANAGER),
        )

        val firOutput = if (configuration.useLightTree) {
            phaseContext.firFrontendWithLightTree(environment)
        } else {
            phaseContext.firFrontendWithPsi(environment)
        }

        if (firOutput !is FirOutput.Full) {
            return null
        }

        return NativeFrontendPipelineArtifact(
            frontendOutput = firOutput.firResult,
            configuration = configuration,
            diagnosticCollector = input.diagnosticCollector,
            firOutput = firOutput,
            environment = environment,
            phaseContext = phaseContext,
        )
    }

    private fun createNativeKlibConfig(
        input: ConfigurationPipelineArtifact,
        environment: KotlinCoreEnvironment,
    ): NativeKlibConfig {
        val configuration = input.configuration

        // Resolve target from configuration
        val target = resolveTarget(configuration)

        // Resolve libraries using explicit paths from configuration
        val resolvedLibraries = resolveLibraries(configuration, target)

        val moduleId = configuration.get(CommonConfigurationKeys.MODULE_NAME) ?: "main"

        return NativeKlibConfig(
            project = environment.project,
            configuration = configuration,
            target = target,
            resolvedLibraries = resolvedLibraries,
            moduleId = moduleId,
            manifestProperties = null,
        )
    }

    private fun resolveTarget(configuration: CompilerConfiguration): KonanTarget {
        val targetName = configuration.get(KonanConfigKeys.TARGET)
        return if (targetName != null) {
            KonanTarget.predefinedTargets[targetName]
                ?: error("Unknown target: $targetName")
        } else {
            HostManager.host
        }
    }

    private fun resolveLibraries(
        configuration: CompilerConfiguration,
        target: KonanTarget,
    ): KotlinLibraryResolveResult {
        val libraryPaths = configuration.getList(KonanConfigKeys.LIBRARY_FILES)
        val includedLibraryPaths = configuration.getList(KonanConfigKeys.INCLUDED_LIBRARIES)

        // Get explicit paths for stdlib and platform libraries
        val stdlibPath = configuration.get(NativeKlibConfigurationKeys.NATIVE_STDLIB_PATH)
        val platformLibrariesPath = configuration.get(NativeKlibConfigurationKeys.NATIVE_PLATFORM_LIBRARIES_PATH)

        val unresolvedLibraries = libraryPaths.toUnresolvedLibraries

        val resolver = createNativeKlibResolver(
            directLibs = libraryPaths + includedLibraryPaths,
            target = target,
            stdlibPath = stdlibPath,
            platformLibrariesPath = platformLibrariesPath,
            logger = configuration.getLogger(),
            zipFileSystemAccessor = configuration.zipFileSystemAccessor
        )

        val additionalLibraries = includedLibraryPaths.map { RequiredUnresolvedLibrary(it) }

        return resolver.resolveWithDependencies(
            unresolvedLibraries + additionalLibraries,
            noStdLib = configuration.getBoolean(KonanConfigKeys.NOSTDLIB),
            noDefaultLibs = configuration.getBoolean(KonanConfigKeys.NODEFAULTLIBS),
            noEndorsedLibs = configuration.getBoolean(KonanConfigKeys.NOENDORSEDLIBS),
            duplicatedUniqueNameStrategy = configuration.get(
                KlibConfigurationKeys.DUPLICATED_UNIQUE_NAME_STRATEGY,
                DuplicatedUniqueNameStrategy.DENY
            ),
        )
    }
}
