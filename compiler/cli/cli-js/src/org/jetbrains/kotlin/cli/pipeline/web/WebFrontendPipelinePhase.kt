/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web

import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.cli.CliDiagnostics.COMPILER_ARGUMENTS_ERROR
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.extensionsStorage
import org.jetbrains.kotlin.cli.hasMessageCollectorErrors
import org.jetbrains.kotlin.cli.js.platformChecker
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.toVfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.incrementalDataProvider
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.ir.backend.js.loadWebKlibs
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.PotentiallyIncorrectPhaseTimeMeasurement

@OptIn(ExperimentalCompilerApi::class)
object WebFrontendPipelinePhase : PipelinePhase<ConfigurationPipelineArtifact, WebFrontendPipelineArtifact>(
    name = "JsFrontendPipelinePhase",
    postActions = setOf(PerformanceNotifications.AnalysisFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: ConfigurationPipelineArtifact): WebFrontendPipelineArtifact? {
        val configuration = input.configuration

        @OptIn(CoreEnvironmentDeprecation::class)
        val environmentForJS = KotlinCoreEnvironment.create(input.rootDisposable, configuration)
        configuration.perfManager?.let {
            @OptIn(PotentiallyIncorrectPhaseTimeMeasurement::class)
            it.notifyCurrentPhaseFinishedIfNeeded()
            it.notifyPhaseStarted(PhaseType.Analysis)
        }
        val diagnosticsCollector = configuration.diagnosticsCollector

        val klibs = loadWebKlibs(configuration, configuration.platformChecker)

        val extensionStorage = configuration.extensionsStorage ?: error("Extensions storage is not registered")
        val groupedSources = collectSources(configuration, environmentForJS.toVfsBasedProjectEnvironment())

        if (
            groupedSources.isEmpty() &&
            !configuration.allowNoSourceFiles &&
            !configuration.jsIncrementalCompilationEnabled
        ) {
            if (!configuration.printVersion) {
                configuration.report(COMPILER_ARGUMENTS_ERROR, "No source files")
            }
            return null
        }

        val analyzedOutput = compileModulesToAnalyzedFir(
            configuration = configuration,
            resolvedLibraries = klibs.all,
            groupedSources = groupedSources,
            libraries = configuration.libraries,
            friendLibraries = configuration.friendLibraries,
            incrementalDataProvider = configuration.incrementalDataProvider,
            extensionStorage = extensionStorage,
            useWasmPlatform = configuration.wasmCompilation,
        )

        return WebFrontendPipelineArtifact(
            analyzedOutput,
            configuration,
            klibs.all,
            hasErrors = configuration.hasMessageCollectorErrors() || diagnosticsCollector.hasErrors,
        )
    }

    private fun compileModulesToAnalyzedFir(
        configuration: CompilerConfiguration,
        resolvedLibraries: List<KotlinLibrary>,
        groupedSources: GroupedKtSources,
        libraries: List<String>,
        friendLibraries: List<String>,
        incrementalDataProvider: IncrementalDataProvider?,
        extensionStorage: CompilerPluginRegistrar.ExtensionStorage,
        useWasmPlatform: Boolean,
    ): AllModulesFrontendOutput {
        // FIR
        @Suppress("UNCHECKED_CAST")
        val extensionRegistrars = extensionStorage[FirExtensionRegistrarAdapter] as List<FirExtensionRegistrar>

        val mainModuleName = configuration.moduleName!!
        val escapedMainModuleName = Name.special("<$mainModuleName>")
        val dependencyList = DependencyListForCliModule.build(escapedMainModuleName) {
            dependencies(libraries)
            friendDependencies(friendLibraries)
            // TODO: !!! dependencies module data?
        }

        val sessionsWithSources = if (useWasmPlatform) {
            prepareWasmSessions(
                groupedSources, configuration, escapedMainModuleName,
                resolvedLibraries, dependencyList, extensionRegistrars,
                incrementalDataProvider = incrementalDataProvider
            )
        } else {
            prepareJsSessions(
                groupedSources, configuration, escapedMainModuleName,
                resolvedLibraries, dependencyList, extensionRegistrars,
                incrementalDataProvider = incrementalDataProvider
            )
        }

        val outputs = sessionsWithSources.map { (session, files) -> buildResolveAndCheckFir(session, files, configuration) }
        outputs.runPlatformCheckers(configuration.diagnosticsCollector)
        return AllModulesFrontendOutput(outputs)
    }
}
