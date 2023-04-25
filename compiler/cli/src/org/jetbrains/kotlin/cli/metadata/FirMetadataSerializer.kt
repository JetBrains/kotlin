/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.metadata

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.kotlin.analyzer.common.CommonPlatformAnalyzerServices
import org.jetbrains.kotlin.backend.common.CommonJsKLibResolver
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.createContextForIncrementalCompilation
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.collectSources
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.createContextForIncrementalCompilation
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.createIncrementalCompilationScope
import org.jetbrains.kotlin.cli.jvm.compiler.toAbstractProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.K2MetadataConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.BinaryModuleData
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.pipeline.ModuleCompilerAnalyzedOutput
import org.jetbrains.kotlin.fir.pipeline.buildFirFromKtFiles
import org.jetbrains.kotlin.fir.pipeline.buildFirViaLightTree
import org.jetbrains.kotlin.fir.pipeline.resolveAndCheckFir
import org.jetbrains.kotlin.fir.serialization.FirKLibSerializerExtension
import org.jetbrains.kotlin.fir.serialization.serializeSingleFirFile
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.metadata.KlibMetadataHeaderFlags
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.util.DummyLogger
import java.io.File

internal class FirMetadataSerializer(
    configuration: CompilerConfiguration,
    environment: KotlinCoreEnvironment
) : AbstractMetadataSerializer<List<ModuleCompilerAnalyzedOutput>>(configuration, environment) {
    override fun analyze(): List<ModuleCompilerAnalyzedOutput>? {
        val performanceManager = environment.configuration.getNotNull(CLIConfigurationKeys.PERF_MANAGER)
        performanceManager.notifyAnalysisStarted()

        val configuration = environment.configuration
        val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        val rootModuleName = Name.special("<${configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME)}>")
        val isLightTree = configuration.getBoolean(CommonConfigurationKeys.USE_LIGHT_TREE)

        val binaryModuleData = BinaryModuleData.initialize(
            rootModuleName,
            CommonPlatforms.defaultCommonPlatform,
            CommonPlatformAnalyzerServices
        )
        val libraryList = DependencyListForCliModule.build(binaryModuleData) {
            val refinedPaths = configuration.get(K2MetadataConfigurationKeys.REFINES_PATHS)?.map { File(it) }.orEmpty()
            dependencies(configuration.jvmClasspathRoots.filter { it !in refinedPaths }.map { it.toPath() })
            dependencies(configuration.jvmModularRoots.map { it.toPath() })
            friendDependencies(configuration[K2MetadataConfigurationKeys.FRIEND_PATHS] ?: emptyList())
            dependsOnDependencies(refinedPaths.map { it.toPath() })
        }

        val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter()

        val klibFiles = configuration.get(CLIConfigurationKeys.CONTENT_ROOTS).orEmpty()
            .filterIsInstance<JvmClasspathRoot>()
            .filter { it.file.isDirectory || it.file.extension == "klib" }
            .map { it.file.absolutePath }
        val resolvedLibraries = CommonJsKLibResolver.resolve(klibFiles, DummyLogger).getFullResolvedList()

        val outputs = if (isLightTree) {
            val projectEnvironment = environment.toAbstractProjectEnvironment() as VfsBasedProjectEnvironment
            var librariesScope = projectEnvironment.getSearchScopeForProjectLibraries()
            val groupedSources = collectSources(configuration, projectEnvironment, messageCollector)
            val extensionRegistrars = FirExtensionRegistrar.getInstances(projectEnvironment.project)
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
            val extensionRegistrars = FirExtensionRegistrar.getInstances(projectEnvironment.project)
            val psiFiles = environment.getSourceFiles()
            val sourceScope =
                projectEnvironment.getSearchScopeByPsiFiles(psiFiles) + projectEnvironment.getSearchScopeForProjectJavaSources()
            val providerAndScopeForIncrementalCompilation = createContextForIncrementalCompilation(
                projectEnvironment,
                configuration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS),
                configuration,
                configuration.get(JVMConfigurationKeys.MODULES)?.map(::TargetId),
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


        return if (diagnosticsReporter.hasErrors) {
            val renderDiagnosticNames = configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
            FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector, renderDiagnosticNames)
            null
        } else {
            outputs
        }.also {
            performanceManager.notifyAnalysisFinished()
        }
    }

    override fun serialize(analysisResult: List<ModuleCompilerAnalyzedOutput>, destDir: File) {
        val fragments = mutableMapOf<String, MutableList<ByteArray>>()

        for (output in analysisResult) {
            val (session, scopeSession, fir) = output

            val languageVersionSettings = environment.configuration.languageVersionSettings
            for (firFile in fir) {
                val packageFragment = serializeSingleFirFile(
                    firFile,
                    session,
                    scopeSession,
                    actualizedExpectDeclarations = null,
                    FirKLibSerializerExtension(
                        session, metadataVersion, constValueProvider = null,
                        allowErrorTypes = false, exportKDoc = false
                    ),
                    languageVersionSettings,
                )
                fragments.getOrPut(firFile.packageFqName.asString()) { mutableListOf() }.add(packageFragment.toByteArray())
            }
        }

        val header = KlibMetadataProtoBuf.Header.newBuilder()
        header.moduleName = analysisResult.last().session.moduleData.name.asString()

        if (configuration.languageVersionSettings.isPreRelease()) {
            header.flags = KlibMetadataHeaderFlags.PRE_RELEASE
        }

        val fragmentNames = mutableListOf<String>()
        val fragmentParts = mutableListOf<List<ByteArray>>()

        for ((fqName, fragment) in fragments.entries.sortedBy { it.key }) {
            fragmentNames += fqName
            fragmentParts += fragment
            header.addPackageFragmentName(fqName)
        }

        val module = header.build().toByteArray()

        val serializedMetadata = SerializedMetadata(module, fragmentParts, fragmentNames)

        buildKotlinMetadataLibrary(configuration, serializedMetadata, destDir)
    }
}
