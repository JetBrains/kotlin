/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.metadata

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.collectSources
import org.jetbrains.kotlin.cli.common.fileBelongsToModuleForLt
import org.jetbrains.kotlin.cli.common.fileBelongsToModuleForPsi
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.isCommonSourceForLt
import org.jetbrains.kotlin.cli.common.isCommonSourceForPsi
import org.jetbrains.kotlin.cli.common.messages.toLogger
import org.jetbrains.kotlin.cli.common.prepareCommonSessions
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.createContextForIncrementalCompilation
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.createIncrementalCompilationScope
import org.jetbrains.kotlin.cli.jvm.compiler.toVfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.K2MetadataConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.BinaryModuleData
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.ModuleCompilerAnalyzedOutput
import org.jetbrains.kotlin.fir.pipeline.buildFirFromKtFiles
import org.jetbrains.kotlin.fir.pipeline.buildFirViaLightTree
import org.jetbrains.kotlin.fir.pipeline.resolveAndCheckFir
import org.jetbrains.kotlin.fir.pipeline.runPlatformCheckers
import org.jetbrains.kotlin.library.metadata.resolver.impl.KotlinResolvedLibraryImpl
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import java.io.File

internal abstract class AbstractFirMetadataSerializer(
    configuration: CompilerConfiguration,
    environment: KotlinCoreEnvironment
) : AbstractMetadataSerializer<List<ModuleCompilerAnalyzedOutput>>(configuration, environment) {
    override fun analyze(): List<ModuleCompilerAnalyzedOutput>? {
        val performanceManager = environment.configuration.getNotNull(CLIConfigurationKeys.PERF_MANAGER)
        performanceManager.notifyAnalysisStarted()

        val configuration = environment.configuration
        val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        val rootModuleName = Name.special("<${configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME)}>")
        val isLightTree = configuration.getBoolean(CommonConfigurationKeys.USE_LIGHT_TREE)

        val binaryModuleData = BinaryModuleData.Companion.initialize(
            rootModuleName,
            CommonPlatforms.defaultCommonPlatform,
        )
        val libraryList = DependencyListForCliModule.Companion.build(binaryModuleData) {
            val refinedPaths = configuration.get(K2MetadataConfigurationKeys.REFINES_PATHS)?.map { File(it) }.orEmpty()
            dependencies(configuration.jvmClasspathRoots.filter { it !in refinedPaths }.map { it.toPath() })
            dependencies(configuration.jvmModularRoots.map { it.toPath() })
            friendDependencies(configuration[K2MetadataConfigurationKeys.FRIEND_PATHS] ?: emptyList())
            dependsOnDependencies(refinedPaths.map { it.toPath() })
        }

        val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter(messageCollector)

        val klibFiles = configuration.get(CLIConfigurationKeys.CONTENT_ROOTS).orEmpty()
            .filterIsInstance<JvmClasspathRoot>()
            .filter { it.file.isDirectory || it.file.extension == "klib" }
            .map { it.file.absolutePath }

        val logger = messageCollector.toLogger()

        // TODO: This is a workaround for KT-63573. Revert it back when KT-64169 is fixed.
//        val resolvedLibraries = CommonKLibResolver.resolve(klibFiles, logger).getFullResolvedList()
        val resolvedLibraries = klibFiles.map {
            KotlinResolvedLibraryImpl(
                resolveSingleFileKlib(
                    org.jetbrains.kotlin.konan.file.File(it),
                    logger
                )
            )
        }

        val outputs = if (isLightTree) {
            val projectEnvironment = environment.toVfsBasedProjectEnvironment() as VfsBasedProjectEnvironment
            var librariesScope = projectEnvironment.getSearchScopeForProjectLibraries()
            val groupedSources = collectSources(configuration, projectEnvironment, messageCollector)
            val extensionRegistrars = FirExtensionRegistrar.Companion.getInstances(projectEnvironment.project)
            val ltFiles = groupedSources.let { it.commonSources + it.platformSources }.toList()
            val incrementalCompilationScope = createIncrementalCompilationScope(
                configuration,
                projectEnvironment,
                incrementalExcludesScope = null
            )?.also { librariesScope -= it }
            val sessionsWithSources = prepareCommonSessions(
                ltFiles, configuration, projectEnvironment, rootModuleName, extensionRegistrars, librariesScope,
                libraryList, resolvedLibraries, groupedSources.isCommonSourceForLt, groupedSources.fileBelongsToModuleForLt,
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
                val firFiles = session.buildFirViaLightTree(files, diagnosticsReporter, performanceManager::addSourcesStats)
                resolveAndCheckFir(session, firFiles, diagnosticsReporter)
            }
        } else {
            val projectEnvironment = VfsBasedProjectEnvironment(
                environment.project,
                VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
            ) { environment.createPackagePartProvider(it) }
            var librariesScope = projectEnvironment.getSearchScopeForProjectLibraries()
            val extensionRegistrars = FirExtensionRegistrar.Companion.getInstances(projectEnvironment.project)
            val psiFiles = environment.getSourceFiles()
            val sourceScope =
                projectEnvironment.getSearchScopeByPsiFiles(psiFiles) + projectEnvironment.getSearchScopeForProjectJavaSources()
            val providerAndScopeForIncrementalCompilation = org.jetbrains.kotlin.cli.jvm.compiler.createContextForIncrementalCompilation(
                projectEnvironment,
                configuration,
                sourceScope
            )
            providerAndScopeForIncrementalCompilation?.precompiledBinariesFileScope?.let {
                librariesScope -= it
            }
            val sessionsWithSources = prepareCommonSessions(
                psiFiles, configuration, projectEnvironment, rootModuleName, extensionRegistrars,
                librariesScope, libraryList, resolvedLibraries, isCommonSourceForPsi, fileBelongsToModuleForPsi,
                createProviderAndScopeForIncrementalCompilation = { providerAndScopeForIncrementalCompilation }
            )

            sessionsWithSources.map { (session, files) ->
                val firFiles = session.buildFirFromKtFiles(files)
                resolveAndCheckFir(session, firFiles, diagnosticsReporter)
            }
        }

        outputs.runPlatformCheckers(diagnosticsReporter)

        val renderDiagnosticNames = configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
        FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector, renderDiagnosticNames)

        return if (messageCollector.hasErrors()) {
            null
        } else {
            outputs
        }.also {
            performanceManager.notifyAnalysisFinished()
        }
    }

    abstract override fun serialize(analysisResult: List<ModuleCompilerAnalyzedOutput>, destDir: File): OutputInfo?
}
