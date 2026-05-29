/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.cli.pipeline.metadata

import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.backend.common.loadMetadataKlibs
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.K2MetadataConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.asKtFilesList
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.config.useLightTree
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.PotentiallyIncorrectPhaseTimeMeasurement
import java.io.File

object MetadataFrontendPipelinePhase : PipelinePhase<ConfigurationPipelineArtifact, MetadataFrontendPipelineArtifact>(
    name = "MetadataFrontendPipelinePhase",
    postActions = setOf(PerformanceNotifications.AnalysisFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: ConfigurationPipelineArtifact): MetadataFrontendPipelineArtifact {
        val (configuration, rootDisposable) = input
        val diagnosticsReporter = configuration.diagnosticsCollector
        val rootModuleName = Name.special("<${configuration.moduleName!!}>")
        val isLightTree = configuration.getBoolean(CommonConfigurationKeys.USE_LIGHT_TREE)

        val libraryList = DependencyListForCliModule.build(rootModuleName) {
            val refinedPaths = configuration.get(K2MetadataConfigurationKeys.REFINES_PATHS)?.map { File(it) }.orEmpty()
            dependencies(configuration.jvmClasspathRoots.filter { it !in refinedPaths }.map { it.path })
            dependencies(configuration.jvmModularRoots.map { it.path })
            friendDependencies(configuration[K2MetadataConfigurationKeys.FRIEND_PATHS] ?: emptyList())
            dependsOnDependencies(refinedPaths.map { it.path })
        }

        val klibs: List<KotlinLibrary> = loadMetadataKlibs(
            libraryPaths = configuration.contentRoots.mapNotNull { (it as? JvmClasspathRoot)?.file?.path },
            configuration = configuration
        ).all

        val perfManager = configuration.perfManager
        val environment = KotlinCoreEnvironment.createForProduction(
            rootDisposable,
            configuration,
            EnvironmentConfigFiles.METADATA_CONFIG_FILES
        )
        perfManager?.let {
            @OptIn(PotentiallyIncorrectPhaseTimeMeasurement::class)
            it.notifyCurrentPhaseFinishedIfNeeded()
            it.notifyPhaseStarted(PhaseType.Analysis)
        }

        val extensionRegistrars = configuration.getCompilerExtensions(FirExtensionRegistrar)

        val projectEnvironment = environment.toVfsBasedProjectEnvironment()
        val [librariesScope, incrementalCompilationContext] = prepareIncrementalCompilationContextAndLibrariesScope(
            configuration,
            projectEnvironment,
            previousStepsSymbolProviders = emptyList(),
            incrementalExcludesScope = null
        )

        val groupedSources = collectSources(configuration, projectEnvironment)

        val sourceFiles = when {
            isLightTree -> groupedSources.let { it.commonSources + it.platformSources }.toList()
            else -> environment.getSourceFiles().also { ktFiles ->
                perfManager?.addSourcesStats(ktFiles.size, environment.countLinesOfCode(ktFiles))
                for (ktFile in ktFiles) {
                    AnalyzerWithCompilerReport.reportSyntaxErrors(ktFile, diagnosticsReporter)
                }
            }.map { KtPsiSourceFile(it) }
        }

        val sessionsWithSources = prepareMetadataSessions(
            sourceFiles,
            configuration,
            projectEnvironment,
            rootModuleName,
            extensionRegistrars,
            librariesScope,
            libraryList,
            resolvedLibraries = klibs,
            isCommonSource = groupedSources.isCommonSourceForLt,
            fileBelongsToModule = groupedSources.fileBelongsToModuleForLt,
            incrementalCompilationContext,
        )

        val outputs = sessionsWithSources.map { (session, files) ->
            val firFiles = when {
                isLightTree -> session.buildFirViaLightTree(files, diagnosticsReporter) { files, lines ->
                    perfManager?.addSourcesStats(files, lines)
                }
                else -> session.buildFirFromKtFiles(files.map { (it as KtPsiSourceFile).psiFile as KtFile })
            }
            resolveAndCheckFir(session, firFiles, diagnosticsReporter)
        }

        outputs.runPlatformCheckers(diagnosticsReporter)

        when (configuration.useLightTree) {
            true -> outputs.all { checkKotlinPackageUsageForLightTree(configuration, it.fir) }
            false -> checkKotlinPackageUsageForPsi(configuration, sourceFiles.asKtFilesList())
        }

        FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, configuration)
        return MetadataFrontendPipelineArtifact(
            AllModulesFrontendOutput(outputs),
            configuration,
            sourceFiles
        )
    }
}
