/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.backend.common.loadMetadataKlibs
import org.jetbrains.kotlin.cli.common.messages.SyntaxErrorReporter
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.countLinesOfCode
import org.jetbrains.kotlin.cli.pipeline.jvm.asKtFilesList
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.CliOnlyLanguageVersionSettingsCheckers
import org.jetbrains.kotlin.fir.checkers.registerExperimentalCheckers
import org.jetbrains.kotlin.fir.checkers.registerExtraCommonCheckers
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.lightTree.isScript
import org.jetbrains.kotlin.fir.pipeline.SingleModuleFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.buildFirFromKtFiles
import org.jetbrains.kotlin.fir.pipeline.buildFirViaLightTree
import org.jetbrains.kotlin.fir.pipeline.resolveAndCheckFir
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.syntheticFunctionInterfacesSymbolProvider
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.fir.session.AbstractFirMetadataSessionFactory.JarMetadataProviderComponents
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.incremental.js.KmpIncrementalDataProvider
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.library.impl.klibIrComponentFromFiles
import org.jetbrains.kotlin.load.kotlin.PackageAndMetadataPartProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import org.jetbrains.kotlin.wasm.config.wasmTarget

/**
 * Builds the raw FIR of [files] with the raw FIR builder of [CompilerConfiguration.parserMode], then resolves it and runs
 * the common checkers. Syntax errors and source statistics are reported while the raw FIR is built.
 *
 * [files] must be the sources of [collectSources], which match the parser mode.
 */
fun buildResolveAndCheckFir(
    session: FirSession,
    files: List<KtSourceFile>,
    configuration: CompilerConfiguration,
): SingleModuleFrontendOutput {
    val diagnosticsCollector = configuration.diagnosticsCollector
    val perfManager = configuration.perfManager
    val reportFilesAndLines = if (perfManager == null) null else perfManager::addSourcesStats
    val rawFirFiles = when (val parserMode = configuration.parserMode) {
        ParserMode.Psi -> {
            val ktFiles = files.asKtFilesList()
            ktFiles.forEach { SyntaxErrorReporter.reportSyntaxErrors(it, diagnosticsCollector) }
            reportFilesAndLines?.invoke(ktFiles.size, ktFiles.countLinesOfCode())
            session.buildFirFromKtFiles(ktFiles)
        }
        ParserMode.LightTree, ParserMode.KmpTree -> session.buildFirViaLightTree(
            files, diagnosticsCollector, useMultiplatformParsing = parserMode == ParserMode.KmpTree, reportFilesAndLines,
        )
    }
    return resolveAndCheckFir(session, rawFirFiles, diagnosticsCollector)
}

/**
 * Creates library session and sources session for JS platform
 * Number of created session depends on mode of MPP:
 *   - disabled
 *   - legacy (one platform and one common module)
 *   - HMPP (multiple number of modules)
 */
fun prepareJsSessions(
    sources: GroupedKtSources,
    configuration: CompilerConfiguration,
    rootModuleName: Name,
    resolvedLibraries: List<KotlinLibrary>,
    libraryList: DependencyListForCliModule,
    extensionRegistrars: List<FirExtensionRegistrar>,
    incrementalDataProvider: IncrementalDataProvider?,
): List<SessionWithSources> {
    return prepareKlibSessions(
        FirJsSessionFactory, JsPlatforms.defaultJsPlatform, sources, configuration, rootModuleName, resolvedLibraries,
        libraryList, extensionRegistrars, metadataCompilationMode = false, incrementalDataProvider
    )
}

/**
 * Creates library session and sources session for Native platform
 * Number of created session depends on mode of MPP:
 *   - disabled
 *   - legacy (one platform and one common module)
 *   - HMPP (multiple number of modules)
 */
fun prepareNativeSessions(
    sources: GroupedKtSources,
    configuration: CompilerConfiguration,
    rootModuleName: Name,
    resolvedLibraries: List<KotlinLibrary>,
    libraryList: DependencyListForCliModule,
    extensionRegistrars: List<FirExtensionRegistrar>,
    metadataCompilationMode: Boolean,
): List<SessionWithSources> {
    return prepareKlibSessions(
        if (metadataCompilationMode) FirNativeSessionFactory.ForMetadata else FirNativeSessionFactory,
        NativePlatforms.unspecifiedNativePlatform, sources, configuration, rootModuleName, resolvedLibraries,
        libraryList, extensionRegistrars, metadataCompilationMode, incrementalDataProvider = null
    )
}

/**
 * Creates library session and sources session for Wasm platform
 * Number of created session depends on mode of MPP:
 *   - disabled
 *   - legacy (one platform and one common module)
 *   - HMPP (multiple number of modules)
 */
fun prepareWasmSessions(
    sources: GroupedKtSources,
    configuration: CompilerConfiguration,
    rootModuleName: Name,
    resolvedLibraries: List<KotlinLibrary>,
    libraryList: DependencyListForCliModule,
    extensionRegistrars: List<FirExtensionRegistrar>,
    incrementalDataProvider: IncrementalDataProvider?,
): List<SessionWithSources> {
    val platform = when (configuration.get(WasmConfigurationKeys.WASM_TARGET, WasmTarget.JS)) {
        WasmTarget.JS -> WasmPlatforms.wasmJs
        WasmTarget.WASI -> WasmPlatforms.wasmWasi
    }
    return prepareKlibSessions(
        FirWasmSessionFactory.of(configuration.wasmTarget),
        platform,
        sources,
        configuration,
        rootModuleName,
        resolvedLibraries,
        libraryList,
        extensionRegistrars,
        metadataCompilationMode = false,
        incrementalDataProvider,
    )
}

private fun prepareKlibSessions(
    sessionFactory: AbstractFirKlibSessionFactory<*>,
    platform: TargetPlatform,
    sources: GroupedKtSources,
    configuration: CompilerConfiguration,
    rootModuleName: Name,
    resolvedLibraries: List<KotlinLibrary>,
    libraryList: DependencyListForCliModule,
    extensionRegistrars: List<FirExtensionRegistrar>,
    metadataCompilationMode: Boolean,
    incrementalDataProvider: IncrementalDataProvider?,
): List<SessionWithSources> {
    return SessionConstructionUtils.prepareSessions(
        sources, configuration, rootModuleName, platform,
        metadataCompilationMode, libraryList, extensionRegistrars, createSeparateSessionForScripts = false,
        createMetadataSessionFactoryContextForHmppCommonLibrarySession = {
            AbstractFirMetadataSessionFactory.Context(
                createJvmContext = { shouldNotBeCalled() },
                createJsContext = { FirJsSessionFactory.Context(configuration) }
            )
        },
        createSharedLibrarySession = {
            sessionFactory.createSharedLibrarySession(
                rootModuleName,
                configuration,
                extensionRegistrars,
            )
        },
        createLibrarySession = { sharedLibrarySession ->
            sessionFactory.createLibrarySession(
                resolvedLibraries,
                sharedLibrarySession,
                libraryList.moduleDataProvider,
                extensionRegistrars,
                configuration,
            )
        },
        createSourceSession = { moduleData, kmpModuleKind, sessionConfigurator ->
            sessionFactory.createSourceSession(
                moduleData,
                extensionRegistrars,
                configuration,
                kmpModuleKind,
                icData = incrementalDataProvider?.toKlibIcData(moduleData, kmpModuleKind),
                init = sessionConfigurator,
            )
        },
        additionalProvidersForMetadataLibrarySessionsInHmppMode = { session, moduleDataProvider, scopeProvider, libraries, _, _ ->
            sessionFactory.createAdditionalDependencyProviders(session, moduleDataProvider, scopeProvider, libraries)
        },
    )
}

private fun IncrementalDataProvider.toKlibIcData(moduleData: FirModuleData, kmpModuleKind: KmpModuleKind): KlibIcData =
    when (kmpModuleKind) {
        KmpModuleKind.SingleModule,
        KmpModuleKind.LeafRegularModule,
        KmpModuleKind.LeafHmppModule,
            -> {
            val inlineData = serializedIrInlineFiles.entries.map {
                SerializedIrFile(
                    it.value.fileData,
                    FqName.ROOT.asString(), // the exact `fqName` and `path` do not matter for our use case
                    it.key.path,
                    it.value.types,
                    it.value.signatures,
                    it.value.strings,
                    it.value.bodies,
                    it.value.declarations,
                    it.value.debugInfo,
                    it.value.fileEntries
                )
            }
            KlibIcData(
                KlibIcMetadataComponent(compiledPackageParts.mapValues { [_, result] -> result.metadata }),
                klibIrComponentFromFiles(inlineData)
            )
        }

        KmpModuleKind.NonLeafRegularModule,
        KmpModuleKind.NonLeafHmppModule,
            -> {
            val kmpProvider = this as? KmpIncrementalDataProvider
                ?: error("Non-leaf KMP module '${moduleData.name}' requires a KmpIncrementalDataProvider, got ${this::class.simpleName}")

            KlibIcData(KlibIcMetadataComponent(kmpProvider.commonMetadata(moduleData.name.asStringStripSpecialMarkers())))
        }
    }

/**
 * Creates library session and sources session for Common platform (for metadata compilation)
 * Number of created sessions is always one, in this mode modules are compiled against compiled
 *   metadata of dependent modules
 */
fun prepareMetadataSessions(
    sources: GroupedKtSources,
    configuration: CompilerConfiguration,
    projectEnvironment: VfsBasedProjectEnvironment,
    rootModuleName: Name,
    extensionRegistrars: List<FirExtensionRegistrar>,
    librariesClasspath: JvmClasspath,
    libraryList: DependencyListForCliModule,
    resolvedLibraries: List<KotlinLibrary>,
    incrementalCompilationContext: IncrementalCompilationContext?,
): List<SessionWithSources> {
    val packagePartProvider = projectEnvironment.getPackagePartProvider(librariesClasspath) as PackageAndMetadataPartProvider
    val languageVersionSettings = configuration.languageVersionSettings
    val targetPlatform = configuration.targetPlatform ?: CommonPlatforms.defaultCommonPlatform
    val sessionFactory = FirMetadataSessionFactory(targetPlatform)
    val context = AbstractFirMetadataSessionFactory.Context(
        createJvmContext = {
            FirJvmSessionFactory.Context(
                configuration,
                projectEnvironment,
                librariesClasspath,
                // A metadata compilation builds no Java facade: this context only registers the JVM session
                // components, `FirMetadataSessionFactory` never calls `FirJvmSessionFactory.create*Session`.
                NoJavaInterop,
                registerJvmDeserializationExtension = false,
            )
        },
        createJsContext = { FirJsSessionFactory.Context(configuration) },
    )
    return SessionConstructionUtils.prepareSessions(
        sources, configuration, rootModuleName, targetPlatform,
        metadataCompilationMode = true, libraryList, extensionRegistrars, createSeparateSessionForScripts = false,
        createMetadataSessionFactoryContextForHmppCommonLibrarySession = { context },
        createSharedLibrarySession = {
            sessionFactory.createSharedLibrarySession(
                rootModuleName,
                languageVersionSettings,
                extensionRegistrars,
                context,
            )
        },
        createLibrarySession = { sharedLibrarySession ->
            sessionFactory.createLibrarySession(
                sharedLibrarySession,
                libraryList.moduleDataProvider,
                extensionRegistrars,
                JarMetadataProviderComponents(
                    packagePartProvider,
                    librariesClasspath,
                    projectEnvironment,
                ),
                resolvedLibraries,
                languageVersionSettings,
                context,
            )
        },
        createSourceSession = { moduleData, kmpModuleKind, sessionConfigurator ->
            sessionFactory.createSourceSession(
                moduleData,
                projectEnvironment,
                incrementalCompilationContext,
                extensionRegistrars,
                configuration,
                context,
                kmpModuleKind,
                init = sessionConfigurator
            )
        }
    )
}

// ---------------------------------------------------- Implementation ----------------------------------------------------

fun interface FirSessionProducer {
    /**
     * @param kmpModuleKind describes the role of [moduleData] in the (possibly multiplatform) module
     * hierarchy, which determines how dependency symbol providers are laid out for the created session.
     */
    fun createSession(
        moduleData: FirModuleData,
        kmpModuleKind: KmpModuleKind,
        sessionConfigurator: FirSessionConfigurator.() -> Unit,
    ): FirSession
}

/**
 * Container for abstract, platform-independent utilities for creating FIR sessions for potentially hmpp module
 */
object SessionConstructionUtils {
    fun prepareSessions(
        sources: GroupedKtSources,
        configuration: CompilerConfiguration,
        rootModuleName: Name,
        targetPlatform: TargetPlatform,
        metadataCompilationMode: Boolean,
        libraryList: DependencyListForCliModule,
        extensionRegistrars: List<FirExtensionRegistrar>,
        createSeparateSessionForScripts: Boolean,
        createMetadataSessionFactoryContextForHmppCommonLibrarySession: () -> AbstractFirMetadataSessionFactory.Context,
        additionalProvidersForMetadataLibrarySessionsInHmppMode: AdditionalProvidersSupplierForHmpp? = null,
        createSharedLibrarySession: () -> FirSession,
        createLibrarySession: (sharedLibrarySession: FirSession) -> Unit,
        createSourceSession: FirSessionProducer,
    ): List<SessionWithSources> {
        val languageVersionSettings = configuration.languageVersionSettings
        val allFiles = sources.commonSources + sources.platformSources
        // `dontCreateSeparateSessionForScripts` is set only in tests mode
        val [scripts, nonScriptFiles] = if (createSeparateSessionForScripts && !configuration.dontCreateSeparateSessionForScripts) {
            allFiles.partition { it.isScript() }
        } else {
            emptyList<KtSourceFile>() to allFiles
        }

        val isMppEnabled = languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)
        val hmppModuleStructure = configuration.get(CommonConfigurationKeys.HMPP_MODULE_STRUCTURE)

        val sharedLibrarySession = createSharedLibrarySession()
        createLibrarySession(sharedLibrarySession)
        val extraAnalysisMode = configuration.useFirExtraCheckers
        val experimentalAnalysisMode = configuration.useFirExperimentalCheckers
        val sessionConfigurator: FirSessionConfigurator.() -> Unit = {
            registerComponent(
                FirBuiltinSyntheticFunctionInterfaceProvider::class,
                sharedLibrarySession.syntheticFunctionInterfacesSymbolProvider
            )

            if (extraAnalysisMode) {
                registerExtraCommonCheckers()
            }
            if (experimentalAnalysisMode) {
                registerExperimentalCheckers()
            }
        }

        val nonScriptSessions = when {
            metadataCompilationMode || !isMppEnabled -> {
                listOf(
                    createSingleSession(
                        nonScriptFiles, rootModuleName, libraryList, targetPlatform,
                        sessionConfigurator, createSourceSession,
                        metadataCompilationMode = metadataCompilationMode
                    )
                )
            }

            hmppModuleStructure == null -> createSessionsForLegacyMppProject(
                nonScriptFiles, rootModuleName, libraryList, targetPlatform,
                sessionConfigurator, sources, createSourceSession
            )

            languageVersionSettings.getFlag(AnalysisFlags.hierarchicalMultiplatformCompilation) -> createSessionsForHierarchicalMppProject(
                nonScriptFiles, rootModuleName, hmppModuleStructure, libraryList, configuration,
                extensionRegistrars, sharedLibrarySession, targetPlatform,
                sessionConfigurator, sources, createMetadataSessionFactoryContextForHmppCommonLibrarySession,
                createSourceSession, additionalProvidersForMetadataLibrarySessionsInHmppMode,
            )

            else -> createSessionsForMppProject(
                nonScriptFiles, rootModuleName, hmppModuleStructure, libraryList, targetPlatform,
                sessionConfigurator, sources, createSourceSession
            )
        }

        return if (scripts.isEmpty()) nonScriptSessions
        else nonScriptSessions +
                createScriptsSession(
                    scripts, rootModuleName, libraryList, nonScriptSessions.last().session.moduleData,
                    targetPlatform, sessionConfigurator, createSourceSession
                )
    }

    private fun createScriptsSession(
        scripts: List<KtSourceFile>,
        rootModuleName: Name,
        libraryList: DependencyListForCliModule,
        lastModuleData: FirModuleData,
        targetPlatform: TargetPlatform,
        sessionConfigurator: FirSessionConfigurator.() -> Unit,
        createSourceSession: FirSessionProducer,
    ): SessionWithSources =
        createSingleSession(
            scripts, Name.identifier("${rootModuleName.asString()}-scripts"),
            @OptIn(PrivateSessionConstructor::class)
            DependencyListForCliModule(
                libraryList.regularDependencies,
                listOf(lastModuleData),
                libraryList.friendDependencies,
                libraryList.moduleDataProvider
            ),
            targetPlatform,
            sessionConfigurator, createSourceSession,
            metadataCompilationMode = false
        )

    private fun createSingleSession(
        files: List<KtSourceFile>,
        rootModuleName: Name,
        libraryList: DependencyListForCliModule,
        targetPlatform: TargetPlatform,
        sessionConfigurator: FirSessionConfigurator.() -> Unit,
        sourceSessionProducer: FirSessionProducer,
        metadataCompilationMode: Boolean,
    ): SessionWithSources {
        val platformModuleData = FirSourceModuleData(
            rootModuleName,
            libraryList.regularDependencies,
            libraryList.dependsOnDependencies,
            libraryList.friendDependencies,
            targetPlatform,
            isCommon = metadataCompilationMode,
        )

        val session = sourceSessionProducer.createSession(platformModuleData, kmpModuleKind = KmpModuleKind.SingleModule) {
            sessionConfigurator()
            useCheckers(CliOnlyLanguageVersionSettingsCheckers)
        }
        return SessionWithSources(session, files)
    }

    private fun createSessionsForLegacyMppProject(
        files: List<KtSourceFile>,
        rootModuleName: Name,
        libraryList: DependencyListForCliModule,
        targetPlatform: TargetPlatform,
        sessionConfigurator: FirSessionConfigurator.() -> Unit,
        sources: GroupedKtSources,
        sourceSessionProducer: FirSessionProducer,
    ): List<SessionWithSources> {
        val commonModuleData = FirSourceModuleData(
            Name.identifier("${rootModuleName.asString()}-common"),
            libraryList.regularDependencies,
            listOf(),
            libraryList.friendDependencies,
            targetPlatform,
            isCommon = true
        )

        val platformModuleData = FirSourceModuleData(
            rootModuleName,
            libraryList.regularDependencies,
            listOf(commonModuleData),
            libraryList.friendDependencies,
            targetPlatform,
            isCommon = false
        )

        val commonFiles = mutableListOf<KtSourceFile>()
        val platformFiles = mutableListOf<KtSourceFile>()
        for (file in files) {
            (if (sources.isCommonSource(file)) commonFiles else platformFiles).add(file)
        }

        val commonSession = sourceSessionProducer.createSession(
            commonModuleData,
            kmpModuleKind = KmpModuleKind.NonLeafRegularModule,
            sessionConfigurator
        )
        val platformSession = sourceSessionProducer.createSession(
            platformModuleData,
            kmpModuleKind = KmpModuleKind.LeafRegularModule
        ) {
            sessionConfigurator()
            // The CLI session might contain an opt-in for an annotation that's defined in the platform module.
            // Therefore, only run the opt-in LV checker on the platform module.
            useCheckers(CliOnlyLanguageVersionSettingsCheckers)
        }

        return listOf(
            SessionWithSources(commonSession, commonFiles),
            SessionWithSources(platformSession, platformFiles)
        )
    }

    private fun createSessionsForMppProject(
        files: List<KtSourceFile>,
        rootModuleName: Name,
        hmppModuleStructure: HmppCliModuleStructure,
        libraryList: DependencyListForCliModule,
        targetPlatform: TargetPlatform,
        sessionConfigurator: FirSessionConfigurator.() -> Unit,
        sources: GroupedKtSources,
        createFirSession: FirSessionProducer,
    ): List<SessionWithSources> {
        val moduleDataForHmppModule = LinkedHashMap<HmppCliModule, FirModuleData>()

        for ([index, module] in hmppModuleStructure.modules.withIndex()) {
            val dependencies = hmppModuleStructure.sourceDependencies[module]
                ?.map { moduleDataForHmppModule.getValue(it) }
                .orEmpty()
            val moduleName = if (index == hmppModuleStructure.modules.lastIndex) {
                rootModuleName
            } else {
                Name.special("<${module.name}>")
            }
            val moduleData = FirSourceModuleData(
                moduleName,
                libraryList.regularDependencies,
                dependsOnDependencies = dependencies,
                libraryList.friendDependencies,
                targetPlatform,
                isCommon = index < hmppModuleStructure.modules.size - 1
            )
            moduleDataForHmppModule[module] = moduleData
        }

        return createSourceSessionsForMppCompilation(
            hmppModuleStructure, moduleDataForHmppModule, files, sources,
            createFirSession, sessionConfigurator, isHmppEnabled = false
        )
    }

    private fun createSessionsForHierarchicalMppProject(
        files: List<KtSourceFile>,
        rootModuleName: Name,
        hmppModuleStructure: HmppCliModuleStructure,
        libraryListForLeafModule: DependencyListForCliModule,
        configuration: CompilerConfiguration,
        extensionRegistrars: List<FirExtensionRegistrar>,
        sharedLibrarySession: FirSession,
        targetPlatform: TargetPlatform,
        sessionConfigurator: FirSessionConfigurator.() -> Unit,
        sources: GroupedKtSources,
        createMetadataSessionFactoryContextForHmppCommonLibrarySession: () -> AbstractFirMetadataSessionFactory.Context,
        createFirSession: FirSessionProducer,
        additionalProvidersForMetadataLibrarySessions: AdditionalProvidersSupplierForHmpp?,
    ): List<SessionWithSources> {
        val moduleDataForHmppModule = LinkedHashMap<HmppCliModule, FirModuleData>()

        val metadataSessionFactoryContext = createMetadataSessionFactoryContextForHmppCommonLibrarySession()
        for ([index, module] in hmppModuleStructure.modules.withIndex()) {
            val dependencies = hmppModuleStructure.sourceDependencies[module]
                ?.map { moduleDataForHmppModule.getValue(it) }
                .orEmpty()
            val moduleName = if (index == hmppModuleStructure.modules.lastIndex) {
                rootModuleName
            } else {
                Name.special("<${module.name}>")
            }

            val isLeaf = index == hmppModuleStructure.modules.lastIndex

            val libraryList = if (isLeaf) {
                libraryListForLeafModule
            } else {
                val libPaths = hmppModuleStructure.moduleDependencies[module].orEmpty().toMutableSet()

                /*
                 * It's expected that each hmpp module will contain unique dependencies and none of the metadata klibs
                 * won't appear twice on a different levels of the module graph. If this happens, then we will have
                 * the same library to be present in dependencies twice, so we will deserialize it twice, which will be
                 * slow and potentially could lead to some unexpected problems.
                 *
                 * So to guarantee this contract even if it wasn't satisfied by the caller of the compiler, we
                 * remove library dependency from the module if the same library is already visible for this
                 * module via its source dependency modules.
                 */
                for (dependency in hmppModuleStructure.sourceDependencies[module].orEmpty()) {
                    libPaths -= hmppModuleStructure.moduleDependencies[dependency].orEmpty()
                }

                val friendLibPaths = hmppModuleStructure.friendDependencies[module].orEmpty().toMutableSet()

                val klibs: List<KotlinLibrary> = loadMetadataKlibs(
                    libraryPaths = libPaths.toList() + friendLibPaths,
                    configuration = configuration
                ).all

                DependencyListForCliModule.build(moduleName) {
                    dependencies(libPaths)
                    friendDependencies(friendLibPaths)
                }.also { libraryList ->
                    FirMetadataSessionFactoryForHmppCompilation(
                        configuration.targetPlatform ?: CommonPlatforms.defaultCommonPlatform
                    ).createLibrarySession(
                        sharedLibrarySession,
                        libraryList.moduleDataProvider,
                        extensionRegistrars,
                        jarMetadataProviderComponents = null,
                        klibs,
                        configuration.languageVersionSettings,
                        metadataSessionFactoryContext,
                        additionalProviders = additionalProvidersForMetadataLibrarySessions?.bind(libPaths, friendLibPaths),
                    )
                }
            }

            val moduleData = FirSourceModuleData(
                moduleName,
                dependencies = libraryList.regularDependencies,
                dependsOnDependencies = dependencies,
                friendDependencies = libraryList.friendDependencies,
                targetPlatform,
                isCommon = !isLeaf
            )
            moduleDataForHmppModule[module] = moduleData
        }

        return createSourceSessionsForMppCompilation(
            hmppModuleStructure, moduleDataForHmppModule, files, sources,
            createFirSession, sessionConfigurator, isHmppEnabled = true
        )
    }

    private fun createSourceSessionsForMppCompilation(
        hmppModuleStructure: HmppCliModuleStructure,
        moduleDataForHmppModule: LinkedHashMap<HmppCliModule, FirModuleData>,
        files: List<KtSourceFile>,
        sources: GroupedKtSources,
        sourceSessionProducer: FirSessionProducer,
        sessionConfigurator: FirSessionConfigurator.() -> Unit,
        isHmppEnabled: Boolean,
    ): List<SessionWithSources> {
        return hmppModuleStructure.modules.mapIndexed { i, module ->
            val moduleData = moduleDataForHmppModule.getValue(module)
            val moduleFiles = files.filter { sources.fileBelongsToModule(it, module.name) }
            val isLeafModule = i == hmppModuleStructure.modules.lastIndex
            val kmpModuleKind = when {
                isLeafModule -> KmpModuleKind.leafModule(isHmppEnabled)
                else -> KmpModuleKind.nonLeafModule(isHmppEnabled)
            }
            val session = sourceSessionProducer.createSession(
                moduleData,
                kmpModuleKind = kmpModuleKind
            ) {
                sessionConfigurator()
                // The CLI session might contain an opt-in for an annotation that's defined in one of the modules.
                // The only module that's guaranteed to have a dependency on this module is the last one.
                if (isLeafModule) {
                    useCheckers(CliOnlyLanguageVersionSettingsCheckers)
                }
            }
            SessionWithSources(session, moduleFiles)
        }
    }
}

data class SessionWithSources(val session: FirSession, val files: List<KtSourceFile>)
