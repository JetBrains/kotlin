/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.pipeline

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.toVfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.pipeline.*
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.fir.resolve.ImplicitIntegerCoercionModuleCapability
import org.jetbrains.kotlin.konan.config.konanPrintFiles
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.native.NativeFirstStagePhaseContext
import org.jetbrains.kotlin.native.createNativeKlibConfig

object NativeFrontendPipelinePhase : PipelinePhase<ConfigurationPipelineArtifact, NativeFrontendArtifact>(
    name = "NativeFrontendPhase",
    preActions = setOf(PerformanceNotifications.AnalysisStarted),
    postActions = setOf(PerformanceNotifications.AnalysisFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: ConfigurationPipelineArtifact): NativeFrontendArtifact {
        val (configuration, rootDisposable) = input
        val config = createNativeKlibConfig(configuration)
        val phaseContext = NativeFirstStagePhaseContext(config)

        val environment = KotlinCoreEnvironment.createForProduction(
            rootDisposable,
            configuration,
            EnvironmentConfigFiles.NATIVE_CONFIG_FILES
        )

        val firOutput = phaseContext.firFrontend(environment, configuration)
        return NativeFrontendArtifact(
            firOutput,
            configuration = configuration,
            phaseContext = phaseContext,
        )
    }

    @OptIn(SessionConfiguration::class, ExperimentalCompilerApi::class)
    private inline fun <F> NativeFirstStagePhaseContext.firFrontend(
        configuration: CompilerConfiguration,
        files: List<F>,
        checkSyntaxErrors: (F) -> Unit,
        noinline isCommonSource: (F) -> Boolean,
        noinline fileBelongsToModule: (F, String) -> Boolean,
        buildResolveAndCheckFir: (FirSession, List<F>, BaseDiagnosticsCollector) -> SingleModuleFrontendOutput,
    ): AllModulesFrontendOutput {
        val extensionRegistrars = configuration.getCompilerExtensions(FirExtensionRegistrar)

        // FIR
        val mainModuleName = Name.special("<${config.moduleId}>")
        files.forEach { checkSyntaxErrors(it) }
        val dependencyList = DependencyListForCliModule.build {
            val (interopLibs, regularLibs) = config.loadedKlibs.all.partition { it.isCInteropLibrary() }
            defaultDependenciesSet(mainModuleName) {
                dependencies(regularLibs.map { it.libraryFile.absolutePath })
                friendDependencies(config.friendModuleFiles.map { it.absolutePath })
                dependsOnDependencies(config.refinesModuleFiles.map { it.absolutePath })
            }
            if (interopLibs.isNotEmpty()) {
                val interopModuleData =
                    FirBinaryDependenciesModuleData(
                        Name.special("<regular interop dependencies of $mainModuleName>"),
                        FirModuleCapabilities.create(listOf(ImplicitIntegerCoercionModuleCapability))
                    )
                dependencies(interopModuleData, interopLibs.map { it.libraryFile.absolutePath })
            }
            // TODO: !!! dependencies module data?
        }

        val sessionsWithSources = prepareNativeSessions(
            files,
            configuration,
            mainModuleName,
            config.loadedKlibs.all,
            dependencyList,
            extensionRegistrars,
            metadataCompilationMode = config.metadataKlib,
            isCommonSource = isCommonSource,
            fileBelongsToModule = fileBelongsToModule,
        )

        val outputs = sessionsWithSources.map { (session, sources) ->
            buildResolveAndCheckFir(session, sources, configuration.diagnosticsCollector).also {
                if (config.configuration.konanPrintFiles) {
                    it.fir.forEach { file -> println(file.render()) }
                }
            }
        }

        outputs.runPlatformCheckers(configuration.diagnosticsCollector)
        return AllModulesFrontendOutput(outputs)
    }

    private fun NativeFirstStagePhaseContext.firFrontendWithPsi(
        input: KotlinCoreEnvironment,
        configuration: CompilerConfiguration,
    ): AllModulesFrontendOutput {
        val perfManager = configuration.perfManager
        val ktFiles = input.getSourceFiles()
        perfManager?.addSourcesStats(ktFiles.size, input.countLinesOfCode(ktFiles))
        return firFrontend(
            configuration,
            ktFiles,
            checkSyntaxErrors = {
                AnalyzerWithCompilerReport.reportSyntaxErrors(it, configuration.diagnosticsCollector).isHasErrors
            },
            isCommonSource = isCommonSourceForPsi,
            fileBelongsToModule = fileBelongsToModuleForPsi,
            buildResolveAndCheckFir = { session, files, diagnosticsReporter ->
                buildResolveAndCheckFirFromKtFiles(session, files, diagnosticsReporter)
            },
        )
    }

    private fun NativeFirstStagePhaseContext.firFrontendWithLightTree(
        input: KotlinCoreEnvironment,
        configuration: CompilerConfiguration,
    ): AllModulesFrontendOutput {
        val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        // FIR

        val groupedSources = collectSources(
            configuration,
            input.toVfsBasedProjectEnvironment(),
            messageCollector
        )

        val ktSourceFiles = mutableListOf<KtSourceFile>().apply {
            addAll(groupedSources.commonSources)
            addAll(groupedSources.platformSources)
        }

        return firFrontend(
            configuration,
            ktSourceFiles,
            checkSyntaxErrors = {},
            isCommonSource = { groupedSources.isCommonSourceForLt(it) },
            fileBelongsToModule = { file, it -> groupedSources.fileBelongsToModuleForLt(file, it) },
            buildResolveAndCheckFir = { session, files, diagnosticsReporter ->
                buildResolveAndCheckFirViaLightTree(session, files, diagnosticsReporter, countFilesAndLines = null)
            },
        )
    }

    private fun NativeFirstStagePhaseContext.firFrontend(
        input: KotlinCoreEnvironment,
        configuration: CompilerConfiguration
    ): AllModulesFrontendOutput {
        var output = if (configuration.getBoolean(CommonConfigurationKeys.USE_LIGHT_TREE)) {
            firFrontendWithLightTree(input, configuration)
        } else {
            firFrontendWithPsi(input, configuration)
        }
        output = FrontendFilesForPluginsGenerationPipelinePhase.createFilesWithGeneratedDeclarations(output)
        return output
    }
}
