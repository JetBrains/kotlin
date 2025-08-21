/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.cli.pipeline.metadata

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.backend.common.loadMetadataKlibs
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.isCommonSourceForLt
import org.jetbrains.kotlin.cli.common.isCommonSourceForPsi
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.createContextForIncrementalCompilation
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.createIncrementalCompilationScope
import org.jetbrains.kotlin.cli.jvm.compiler.toVfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.K2MetadataConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.asKtFilesList
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.config.useLightTree
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.PotentiallyIncorrectPhaseTimeMeasurement
import java.io.File
import kotlin.collections.orEmpty

object MetadataFrontendPipelinePhase : PipelinePhase<ConfigurationPipelineArtifact, MetadataFrontendPipelineArtifact>(
    name = "MetadataFrontendPipelinePhase",
    postActions = setOf(PerformanceNotifications.AnalysisFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: ConfigurationPipelineArtifact): MetadataFrontendPipelineArtifact {
        val (configuration, diagnosticsReporter, rootDisposable) = input
        val messageCollector = configuration.messageCollector
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

        val sourceFiles: List<KtSourceFile>

        val outputs = if (isLightTree) {
            val projectEnvironment = environment.toVfsBasedProjectEnvironment()
            var librariesScope = projectEnvironment.getSearchScopeForProjectLibraries()
            val groupedSources = collectSources(configuration, projectEnvironment, messageCollector)
            val extensionRegistrars = FirExtensionRegistrar.Companion.getInstances(projectEnvironment.project)
            val ltFiles = groupedSources.let { it.commonSources + it.platformSources }.toList().also {
                sourceFiles = it
            }
            val incrementalCompilationScope = createIncrementalCompilationScope(
                configuration,
                projectEnvironment,
                incrementalExcludesScope = null
            )?.also { librariesScope -= it }
            val sessionsWithSources = prepareMetadataSessions(
                ltFiles, configuration, projectEnvironment, rootModuleName, extensionRegistrars, librariesScope,
                libraryList, klibs, groupedSources.isCommonSourceForLt, groupedSources.fileBelongsToModuleForLt,
                createProviderAndScopeForIncrementalCompilation = { files ->
                    createContextForIncrementalCompilation(
                        configuration,
                        projectEnvironment,
                        projectEnvironment.getSearchScopeBySourceFiles(files),
                        previousStepsSymbolProviders = emptyList(),
                        incrementalCompilationScope
                    )
                }
            )
            sessionsWithSources.map { (session, files) ->
                val firFiles = session.buildFirViaLightTree(files, diagnosticsReporter) { files, lines ->
                    perfManager?.addSourcesStats(files, lines)
                }
                resolveAndCheckFir(session, firFiles, diagnosticsReporter)
            }
        } else {
            val projectEnvironment = VfsBasedProjectEnvironment(
                environment.project,
                VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
            ) { environment.createPackagePartProvider(it) }
            var librariesScope = projectEnvironment.getSearchScopeForProjectLibraries()
            val extensionRegistrars = FirExtensionRegistrar.Companion.getInstances(projectEnvironment.project)
            val ktFiles = environment.getSourceFiles().also { ktFiles ->
                perfManager?.addSourcesStats(ktFiles.size, environment.countLinesOfCode(ktFiles))
                sourceFiles = ktFiles.map { KtPsiSourceFile(it) }
            }

            for (ktFile in ktFiles) {
                AnalyzerWithCompilerReport.reportSyntaxErrors(ktFile, diagnosticsReporter)
            }

            val sourceScope =
                projectEnvironment.getSearchScopeByPsiFiles(ktFiles) + projectEnvironment.getSearchScopeForProjectJavaSources()
            val providerAndScopeForIncrementalCompilation = org.jetbrains.kotlin.cli.jvm.compiler.createContextForIncrementalCompilation(
                projectEnvironment,
                configuration,
                sourceScope
            )
            providerAndScopeForIncrementalCompilation?.precompiledBinariesFileScope?.let {
                librariesScope -= it
            }
            val sessionsWithSources = prepareMetadataSessions(
                ktFiles, configuration, projectEnvironment, rootModuleName, extensionRegistrars,
                librariesScope, libraryList, klibs, isCommonSourceForPsi, fileBelongsToModuleForPsi,
                createProviderAndScopeForIncrementalCompilation = { providerAndScopeForIncrementalCompilation }
            )

            sessionsWithSources.map { (session, files) ->
                val firFiles = session.buildFirFromKtFiles(files)
                resolveAndCheckFir(session, firFiles, diagnosticsReporter)
            }
        }

        outputs.runPlatformCheckers(diagnosticsReporter)

        when (configuration.useLightTree) {
            true -> outputs.all { checkKotlinPackageUsageForLightTree(configuration, it.fir) }
            false -> checkKotlinPackageUsageForPsi(configuration, sourceFiles.asKtFilesList())
        }

        val renderDiagnosticNames = configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
        FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector, renderDiagnosticNames)
        return MetadataFrontendPipelineArtifact(
            FirResult(outputs),
            configuration,
            diagnosticsReporter,
            sourceFiles
        )
    }
}
