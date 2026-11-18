/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.pipeline

import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.pipeline.*
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.fir.resolve.ImplicitIntegerCoercionModuleCapability
import org.jetbrains.kotlin.konan.config.konanPrintFiles
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.native.NativeFirstStageCompilationConfig
import org.jetbrains.kotlin.native.NativeFirstStagePhaseContext
import kotlin.io.path.absolutePathString

object NativeFrontendPipelinePhase : PipelinePhase<ConfigurationPipelineArtifact, NativeFrontendArtifact>(
    name = "NativeFrontendPhase",
    preActions = setOf(PerformanceNotifications.AnalysisStarted),
    postActions = setOf(PerformanceNotifications.AnalysisFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: ConfigurationPipelineArtifact): NativeFrontendArtifact {
        val (configuration, rootDisposable) = input
        val config = NativeFirstStageCompilationConfig(configuration)
        val phaseContext = NativeFirstStagePhaseContext(config)

        @OptIn(CoreEnvironmentDeprecation::class)
        val environment = KotlinCoreEnvironment.create(rootDisposable, configuration)

        val firOutput = phaseContext.firFrontend(environment.toVfsBasedProjectEnvironment(), configuration)
        return NativeFrontendArtifact(
            firOutput,
            configuration = configuration,
            phaseContext = phaseContext,
        )
    }

    private fun NativeFirstStagePhaseContext.firFrontend(
        environment: VfsBasedProjectEnvironment,
        configuration: CompilerConfiguration,
    ): AllModulesFrontendOutput {
        val groupedSources = collectSources(configuration, environment)
        val extensionRegistrars = configuration.getCompilerExtensions(FirExtensionRegistrar)

        // FIR
        val mainModuleName = Name.special("<${config.moduleId}>")
        val dependencyList = DependencyListForCliModule.build {
            val [interopLibs, regularLibs] = config.loadedKlibs.all.partition { it.isCInteropLibrary() }
            defaultDependenciesSet(mainModuleName) {
                dependencies(regularLibs.map { it.path.absolutePathString() })
                friendDependencies(config.loadedKlibs.friends.map { it.path.absolutePathString() })
                dependsOnDependencies(config.refinesModuleFiles.map { it.absolutePathString() })
            }
            if (interopLibs.isNotEmpty()) {
                val interopModuleData =
                    FirBinaryDependenciesModuleData(
                        Name.special("<regular interop dependencies of $mainModuleName>"),
                        FirModuleCapabilities.create(listOf(ImplicitIntegerCoercionModuleCapability))
                    )
                dependencies(interopModuleData, interopLibs.map { it.path.absolutePathString() })
            }
            // TODO: !!! dependencies module data?
        }

        val sessionsWithSources = prepareNativeSessions(
            groupedSources,
            configuration,
            mainModuleName,
            config.loadedKlibs.all,
            dependencyList,
            extensionRegistrars,
            metadataCompilationMode = config.metadataKlib,
        )

        val outputs = sessionsWithSources.map { (val session, val sources = files) ->
            buildResolveAndCheckFir(session, sources, configuration).also {
                if (config.configuration.konanPrintFiles) {
                    it.fir.forEach { file -> println(file.render()) }
                }
            }
        }

        outputs.runPlatformCheckers(configuration.diagnosticsCollector)
        return FrontendFilesForPluginsGenerationPipelinePhase.createFilesWithGeneratedDeclarations(AllModulesFrontendOutput(outputs))
    }
}
