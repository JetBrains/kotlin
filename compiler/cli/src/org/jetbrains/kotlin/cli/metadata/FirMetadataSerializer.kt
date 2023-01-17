/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.metadata

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.analyzer.common.CommonPlatformAnalyzerServices
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.collectSources
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.createContextForIncrementalCompilation
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.FirModuleDataImpl
import org.jetbrains.kotlin.fir.checkers.registerExtendedCommonCheckers
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.pipeline.ModuleCompilerAnalyzedOutput
import org.jetbrains.kotlin.fir.pipeline.buildFirFromKtFiles
import org.jetbrains.kotlin.fir.pipeline.buildFirViaLightTree
import org.jetbrains.kotlin.fir.pipeline.resolveAndCheckFir
import org.jetbrains.kotlin.fir.serialization.FirElementAwareSerializableStringTable
import org.jetbrains.kotlin.fir.serialization.FirKLibSerializerExtension
import org.jetbrains.kotlin.fir.serialization.serializeSingleFirFile
import org.jetbrains.kotlin.fir.session.FirCommonSessionFactory
import org.jetbrains.kotlin.fir.session.IncrementalCompilationContext
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.metadata.KlibMetadataHeaderFlags
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

internal class FirMetadataSerializer(
    configuration: CompilerConfiguration,
    environment: KotlinCoreEnvironment
) : AbstractMetadataSerializer<ModuleCompilerAnalyzedOutput>(configuration, environment) {
    override fun analyze(): ModuleCompilerAnalyzedOutput? {
        val performanceManager = environment.configuration.getNotNull(CLIConfigurationKeys.PERF_MANAGER)
        performanceManager.notifyAnalysisStarted()

        val configuration = environment.configuration
        val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        val moduleName = Name.special("<${configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME)}>")
        val isLightTree = configuration.getBoolean(CommonConfigurationKeys.USE_LIGHT_TREE)

        val sessionProvider = FirProjectSessionProvider()

        val projectEnvironment: VfsBasedProjectEnvironment
        var librariesScope: AbstractProjectFileSearchScope
        val sourceScope: AbstractProjectFileSearchScope
        val librariesHelperScope: AbstractProjectFileSearchScope
        var psiFiles: List<KtFile>? = null
        var ltFiles: List<KtSourceFile>? = null
        val providerAndScopeForIncrementalCompilation: IncrementalCompilationContext?

        if (isLightTree) {
            projectEnvironment = environment.toAbstractProjectEnvironment() as VfsBasedProjectEnvironment
            librariesScope = projectEnvironment.getSearchScopeForProjectLibraries()
            librariesHelperScope = projectEnvironment.getSearchScopeForProjectLibraries()
            ltFiles = collectSources(configuration, projectEnvironment, messageCollector).let { it.first + it.second }.toList()
            sourceScope = projectEnvironment.getSearchScopeBySourceFiles(ltFiles)
            providerAndScopeForIncrementalCompilation = createContextForIncrementalCompilation(
                configuration,
                projectEnvironment,
                sourceScope,
                previousStepsSymbolProviders = emptyList(),
                incrementalExcludesScope = null
            )?.also { (_, _, precompiledBinariesFileScope) ->
                precompiledBinariesFileScope?.let { librariesScope -= it }
            }
        } else {
            projectEnvironment = VfsBasedProjectEnvironment(
                environment.project,
                VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
            ) { environment.createPackagePartProvider(it) }
            librariesScope = projectEnvironment.getSearchScopeForProjectLibraries()
            librariesHelperScope = librariesScope
            psiFiles = environment.getSourceFiles()
            sourceScope = projectEnvironment.getSearchScopeByPsiFiles(psiFiles) + projectEnvironment.getSearchScopeForProjectJavaSources()
            providerAndScopeForIncrementalCompilation = createContextForIncrementalCompilation(
                projectEnvironment,
                configuration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS),
                configuration,
                configuration.get(JVMConfigurationKeys.MODULES)?.map(::TargetId),
                sourceScope
            )
            providerAndScopeForIncrementalCompilation?.precompiledBinariesFileScope?.let {
                librariesScope -= it
            }
        }

        val libraryList = createFirLibraryListAndSession(
            moduleName.asString(), configuration, projectEnvironment,
            scope = librariesHelperScope, librariesScope = librariesScope, friendPaths = emptyList(), sessionProvider = sessionProvider,
            isJvm = false
        )

        val commonModuleData = FirModuleDataImpl(
            moduleName,
            libraryList.regularDependencies,
            listOf(),
            libraryList.friendsDependencies,
            CommonPlatforms.defaultCommonPlatform,
            CommonPlatformAnalyzerServices
        )
        val project = projectEnvironment.project
        val session = FirCommonSessionFactory.createModuleBasedSession(
            commonModuleData,
            sessionProvider,
            sourceScope,
            projectEnvironment,
            incrementalCompilationContext = providerAndScopeForIncrementalCompilation,
            FirExtensionRegistrar.getInstances(project),
            configuration.languageVersionSettings,
            lookupTracker = configuration.get(CommonConfigurationKeys.LOOKUP_TRACKER),
            enumWhenTracker = configuration.get(CommonConfigurationKeys.ENUM_WHEN_TRACKER),
            needRegisterJavaElementFinder = true,
            registerExtraComponents = {},
            init = {
                if (configuration.getBoolean(CommonConfigurationKeys.USE_FIR_EXTENDED_CHECKERS)) {
                    registerExtendedCommonCheckers()
                }
            }
        )

        val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter()
        val firFiles = if (isLightTree)
            session.buildFirViaLightTree(ltFiles!!, diagnosticsReporter, performanceManager::addSourcesStats)
        else
            session.buildFirFromKtFiles(psiFiles!!)

        val result = resolveAndCheckFir(session, firFiles, diagnosticsReporter)

        return if (diagnosticsReporter.hasErrors) {
            val renderDiagnosticNames = configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
            FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector, renderDiagnosticNames)
            null
        } else {
            result
        }.also {
            performanceManager.notifyAnalysisFinished()
        }
    }

    override fun serialize(analysisResult: ModuleCompilerAnalyzedOutput, destDir: File) {
        val fragments = mutableMapOf<String, MutableList<ByteArray>>()

        val (session, scopeSession, fir) = analysisResult

        val languageVersionSettings = environment.configuration.languageVersionSettings
        for (firFile in fir) {
            val packageFragment = serializeSingleFirFile(
                firFile,
                session,
                scopeSession,
                FirKLibSerializerExtension(session, metadataVersion, FirElementAwareSerializableStringTable()),
                languageVersionSettings
            )
            fragments.getOrPut(firFile.packageFqName.asString()) { mutableListOf() }.add(packageFragment.toByteArray())
        }

        val header = KlibMetadataProtoBuf.Header.newBuilder()
        header.moduleName = session.moduleData.name.asString()

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

