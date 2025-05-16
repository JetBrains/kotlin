/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.createLibraryListForJvm
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.FrontendContext
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelinePhase
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.CliOnlyLanguageVersionSettingsCheckers
import org.jetbrains.kotlin.fir.checkers.registerExperimentalCheckers
import org.jetbrains.kotlin.fir.checkers.registerExtraCommonCheckers
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.syntheticFunctionInterfacesSymbolProvider
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.load.kotlin.PackageAndMetadataPartProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.multiplatform.hmppModuleName
import org.jetbrains.kotlin.resolve.multiplatform.isCommonSource
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys

val isCommonSourceForPsi: (KtFile) -> Boolean = { it.isCommonSource == true }
val fileBelongsToModuleForPsi: (KtFile, String) -> Boolean = { file, moduleName -> file.hmppModuleName == moduleName }

val GroupedKtSources.isCommonSourceForLt: (KtSourceFile) -> Boolean
    get() = { it in commonSources }

val GroupedKtSources.fileBelongsToModuleForLt: (KtSourceFile, String) -> Boolean
    get() = { file, moduleName -> sourcesByModuleName[moduleName].orEmpty().contains(file) }

/**
 * Creates library session and sources session for JVM platform
 * Number of created session depends on mode of MPP:
 *   - disabled
 *   - legacy (one platform and one common module)
 *   - HMPP (multiple number of modules)
 */
@LegacyK2CliPipeline
fun <F> FrontendContext.prepareJvmSessions(
    files: List<F>,
    rootModuleNameAsString: String,
    friendPaths: List<String>,
    librariesScope: AbstractProjectFileSearchScope,
    isCommonSource: (F) -> Boolean,
    isScript: (F) -> Boolean,
    fileBelongsToModule: (F, String) -> Boolean,
    createProviderAndScopeForIncrementalCompilation: (List<F>) -> IncrementalCompilationContext?,
): List<SessionWithSources<F>> {
    val libraryList = createLibraryListForJvm(rootModuleNameAsString, configuration, friendPaths)
    val rootModuleName = Name.special("<$rootModuleNameAsString>")
    return prepareJvmSessions(
        files, rootModuleName, librariesScope, libraryList,
        isCommonSource, isScript, fileBelongsToModule, createProviderAndScopeForIncrementalCompilation
    )
}

@LegacyK2CliPipeline
fun <F> FrontendContext.prepareJvmSessions(
    files: List<F>,
    rootModuleName: Name,
    librariesScope: AbstractProjectFileSearchScope,
    libraryList: DependencyListForCliModule,
    isCommonSource: (F) -> Boolean,
    isScript: (F) -> Boolean,
    fileBelongsToModule: (F, String) -> Boolean,
    createProviderAndScopeForIncrementalCompilation: (List<F>) -> IncrementalCompilationContext?,
): List<SessionWithSources<F>> {
    return JvmFrontendPipelinePhase.prepareJvmSessions(
        files,
        rootModuleName,
        configuration,
        projectEnvironment,
        librariesScope,
        libraryList,
        isCommonSource,
        isScript,
        fileBelongsToModule,
        createProviderAndScopeForIncrementalCompilation,
    )
}


/**
 * Creates library session and sources session for JS platform
 * Number of created session depends on mode of MPP:
 *   - disabled
 *   - legacy (one platform and one common module)
 *   - HMPP (multiple number of modules)
 */
fun <F> prepareJsSessions(
    files: List<F>,
    configuration: CompilerConfiguration,
    rootModuleName: Name,
    resolvedLibraries: List<KotlinLibrary>,
    libraryList: DependencyListForCliModule,
    extensionRegistrars: List<FirExtensionRegistrar>,
    isCommonSource: (F) -> Boolean,
    fileBelongsToModule: (F, String) -> Boolean,
    icData: KlibIcData?,
): List<SessionWithSources<F>> {
    return prepareKlibSessions(
        FirJsSessionFactory, JsPlatforms.defaultJsPlatform, files, configuration, rootModuleName, resolvedLibraries,
        libraryList, extensionRegistrars, isCommonSource, fileBelongsToModule, metadataCompilationMode = false, icData
    )
}

/**
 * Creates library session and sources session for Native platform
 * Number of created session depends on mode of MPP:
 *   - disabled
 *   - legacy (one platform and one common module)
 *   - HMPP (multiple number of modules)
 */
fun <F> prepareNativeSessions(
    files: List<F>,
    configuration: CompilerConfiguration,
    rootModuleName: Name,
    resolvedLibraries: List<KotlinLibrary>,
    libraryList: DependencyListForCliModule,
    extensionRegistrars: List<FirExtensionRegistrar>,
    metadataCompilationMode: Boolean,
    isCommonSource: (F) -> Boolean,
    fileBelongsToModule: (F, String) -> Boolean,
): List<SessionWithSources<F>> {
    return prepareKlibSessions(
        FirNativeSessionFactory, NativePlatforms.unspecifiedNativePlatform, files, configuration, rootModuleName, resolvedLibraries,
        libraryList, extensionRegistrars, isCommonSource, fileBelongsToModule, metadataCompilationMode, icData = null
    )
}

/**
 * Creates library session and sources session for Wasm platform
 * Number of created session depends on mode of MPP:
 *   - disabled
 *   - legacy (one platform and one common module)
 *   - HMPP (multiple number of modules)
 */
fun <F> prepareWasmSessions(
    files: List<F>,
    configuration: CompilerConfiguration,
    rootModuleName: Name,
    resolvedLibraries: List<KotlinLibrary>,
    libraryList: DependencyListForCliModule,
    extensionRegistrars: List<FirExtensionRegistrar>,
    isCommonSource: (F) -> Boolean,
    fileBelongsToModule: (F, String) -> Boolean,
    icData: KlibIcData?,
): List<SessionWithSources<F>> {
    val platform = when (configuration.get(WasmConfigurationKeys.WASM_TARGET, WasmTarget.JS)) {
        WasmTarget.JS -> WasmPlatforms.wasmJs
        WasmTarget.WASI -> WasmPlatforms.wasmWasi
    }
    return prepareKlibSessions(
        FirWasmSessionFactory, platform, files, configuration, rootModuleName, resolvedLibraries, libraryList, extensionRegistrars,
        isCommonSource, fileBelongsToModule, metadataCompilationMode = false, icData,
    )
}

private fun <F> prepareKlibSessions(
    sessionFactory: AbstractFirKlibSessionFactory<*, *>,
    platform: TargetPlatform,
    files: List<F>,
    configuration: CompilerConfiguration,
    rootModuleName: Name,
    resolvedLibraries: List<KotlinLibrary>,
    libraryList: DependencyListForCliModule,
    extensionRegistrars: List<FirExtensionRegistrar>,
    isCommonSource: (F) -> Boolean,
    fileBelongsToModule: (F, String) -> Boolean,
    metadataCompilationMode: Boolean,
    icData: KlibIcData?,
): List<SessionWithSources<F>> {
    return SessionConstructionUtils.prepareSessions(
        files, configuration, rootModuleName, platform,
        metadataCompilationMode, libraryList, isCommonSource, isScript = { false },
        fileBelongsToModule,
        createSharedLibrarySession = { sessionProvider ->
            sessionFactory.createSharedLibrarySession(
                rootModuleName,
                sessionProvider,
                configuration,
                extensionRegistrars,
            )
        },
        createLibrarySession = { sessionProvider, sharedLibrarySession ->
            sessionFactory.createLibrarySession(
                resolvedLibraries,
                sessionProvider,
                sharedLibrarySession,
                libraryList.moduleDataProvider,
                extensionRegistrars,
                configuration,
            )
        }
    ) { _, moduleData, sessionProvider, sessionConfigurator ->
        sessionFactory.createSourceSession(
            moduleData,
            sessionProvider,
            extensionRegistrars,
            configuration,
            icData = icData,
            init = sessionConfigurator,
        )
    }
}

/**
 * Creates library session and sources session for Common platform (for metadata compilation)
 * Number of created sessions is always one, in this mode modules are compiled against compiled
 *   metadata of dependent modules
 */
fun <F> prepareMetadataSessions(
    files: List<F>,
    configuration: CompilerConfiguration,
    projectEnvironment: VfsBasedProjectEnvironment,
    rootModuleName: Name,
    extensionRegistrars: List<FirExtensionRegistrar>,
    librariesScope: AbstractProjectFileSearchScope,
    libraryList: DependencyListForCliModule,
    resolvedLibraries: List<KotlinResolvedLibrary>,
    isCommonSource: (F) -> Boolean,
    fileBelongsToModule: (F, String) -> Boolean,
    createProviderAndScopeForIncrementalCompilation: (List<F>) -> IncrementalCompilationContext?,
): List<SessionWithSources<F>> {
    val packagePartProvider = projectEnvironment.getPackagePartProvider(librariesScope) as PackageAndMetadataPartProvider
    val languageVersionSettings = configuration.languageVersionSettings
    return SessionConstructionUtils.prepareSessions(
        files, configuration, rootModuleName, CommonPlatforms.defaultCommonPlatform,
        metadataCompilationMode = true, libraryList, isCommonSource, isScript = { false }, fileBelongsToModule,
        createSharedLibrarySession = { sessionProvider ->
            FirMetadataSessionFactory.createSharedLibrarySession(
                rootModuleName,
                sessionProvider,
                languageVersionSettings,
                extensionRegistrars,
            )
        },
        createLibrarySession = { sessionProvider, sharedLibrarySession ->
            FirMetadataSessionFactory.createLibrarySession(
                sessionProvider,
                sharedLibrarySession,
                libraryList.moduleDataProvider,
                projectEnvironment,
                extensionRegistrars,
                librariesScope,
                resolvedLibraries,
                packagePartProvider,
                languageVersionSettings,
            )
        }
    ) { moduleFiles, moduleData, sessionProvider, sessionConfigurator ->
        FirMetadataSessionFactory.createSourceSession(
            moduleData,
            sessionProvider,
            projectEnvironment,
            incrementalCompilationContext = createProviderAndScopeForIncrementalCompilation(moduleFiles),
            extensionRegistrars,
            configuration,
            init = sessionConfigurator
        )
    }
}

/**
 * Creates library session and sources session for klib compilation
 * Number of created session depends on mode of MPP:
 *   - disabled
 *   - legacy (one platform and one common module)
 *   - HMPP (multiple number of modules)
 */
fun <F> prepareJKlibSessions(
    projectEnvironment: VfsBasedProjectEnvironment,
    files: List<F>,
    configuration: CompilerConfiguration,
    rootModuleName: Name,
    resolvedLibraries: List<KotlinResolvedLibrary>,
    libraryList: DependencyListForCliModule,
    extensionRegistrars: List<FirExtensionRegistrar>,
    metadataCompilationMode: Boolean,
    librariesScope: AbstractProjectFileSearchScope,
    isCommonSource: (F) -> Boolean,
    fileBelongsToModule: (F, String) -> Boolean,
): List<SessionWithSources<F>> {
    val predefinedJavaComponents = FirSharableJavaComponents(firCachesFactoryForCliMode)
    val packagePartProviderForLibraries = projectEnvironment.getPackagePartProvider(librariesScope)

    return SessionConstructionUtils.prepareSessions(
        files, configuration, rootModuleName, NativePlatforms.unspecifiedNativePlatform,
        metadataCompilationMode, libraryList, isCommonSource, isScript = { false },
        fileBelongsToModule,
        createSharedLibrarySession = { sessionProvider ->
            FirJKlibSessionFactory.createSharedLibrarySession(
                rootModuleName,
                sessionProvider,
                projectEnvironment,
                extensionRegistrars,
                librariesScope,
                packagePartProviderForLibraries,
                configuration.languageVersionSettings,
                predefinedJavaComponents = predefinedJavaComponents,
            )
        },
        createLibrarySession = { sessionProvider, sharedLibrarySession ->
            FirJKlibSessionFactory.createLibrarySession(
                resolvedLibraries.map { it.library },
                sessionProvider,
                sharedLibrarySession,
                libraryList.moduleDataProvider,
                projectEnvironment,
                extensionRegistrars,
                librariesScope,
                packagePartProviderForLibraries,
                configuration.languageVersionSettings,
                predefinedJavaComponents = predefinedJavaComponents,
            )
        }
    ) { moduleFiles, moduleData, sessionProvider, sessionConfigurator ->
        FirJKlibSessionFactory.createSourceSession(
            moduleData = moduleData,
            sessionProvider = sessionProvider,
            javaSourcesScope = projectEnvironment.getSearchScopeForProjectJavaSources(),
            projectEnvironment = projectEnvironment,
            createIncrementalCompilationSymbolProviders = { session -> null},
            extensionRegistrars = extensionRegistrars,
            configuration = configuration,
            predefinedJavaComponents = predefinedJavaComponents,
            needRegisterJavaElementFinder = true,
            init = sessionConfigurator
        )
    }
}

// ---------------------------------------------------- Implementation ----------------------------------------------------

typealias FirSessionProducer<F> = (List<F>, FirModuleData, FirProjectSessionProvider, FirSessionConfigurator.() -> Unit) -> FirSession

/**
 * Container for abstract, platform-independent utilities for creating FIR sessions for potentially hmpp module
 */
object SessionConstructionUtils {
    inline fun <F> prepareSessions(
        files: List<F>,
        configuration: CompilerConfiguration,
        rootModuleName: Name,
        targetPlatform: TargetPlatform,
        metadataCompilationMode: Boolean,
        libraryList: DependencyListForCliModule,
        isCommonSource: (F) -> Boolean,
        isScript: (F) -> Boolean,
        fileBelongsToModule: (F, String) -> Boolean,
        createSharedLibrarySession: (FirProjectSessionProvider) -> FirSession,
        createLibrarySession: (FirProjectSessionProvider, sharedLibrarySession: FirSession) -> FirSession,
        createSourceSession: FirSessionProducer<F>,
    ): List<SessionWithSources<F>> {
        val languageVersionSettings = configuration.languageVersionSettings
        val (scripts, nonScriptFiles) = when (configuration.dontCreateSeparateSessionForScripts) {
            false -> files.partition(isScript)
            // only in tests mode
            true -> emptyList<F>() to files
        }

        val isMppEnabled = languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)
        val hmppModuleStructure = configuration.get(CommonConfigurationKeys.HMPP_MODULE_STRUCTURE)
        val sessionProvider = FirProjectSessionProvider()

        val sharedLibrarySession = createSharedLibrarySession(sessionProvider)
        val librarySession = createLibrarySession(sessionProvider, sharedLibrarySession)
        val extraAnalysisMode = configuration.useFirExtraCheckers
        val experimentalAnalysisMode = configuration.useFirExperimentalCheckers
        val sessionConfigurator: FirSessionConfigurator.() -> Unit = {
            registerComponent(FirBuiltinSyntheticFunctionInterfaceProvider::class, librarySession.syntheticFunctionInterfacesSymbolProvider)

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
                        sessionProvider, sessionConfigurator, createSourceSession
                    )
                )
            }

            hmppModuleStructure == null -> createSessionsForLegacyMppProject(
                nonScriptFiles, rootModuleName, libraryList, targetPlatform,
                sessionProvider, sessionConfigurator, isCommonSource, createSourceSession
            )

            else -> createSessionsForHmppProject(
                nonScriptFiles, rootModuleName, hmppModuleStructure, libraryList, targetPlatform,
                sessionProvider, sessionConfigurator, fileBelongsToModule, createSourceSession
            )
        }

        return if (scripts.isEmpty()) nonScriptSessions
        else nonScriptSessions +
                createScriptsSession(
                    scripts, rootModuleName, libraryList, nonScriptSessions.last().session.moduleData,
                    targetPlatform, sessionProvider, sessionConfigurator, createSourceSession
                )
    }

    inline fun <F> createScriptsSession(
        scripts: List<F>,
        rootModuleName: Name,
        libraryList: DependencyListForCliModule,
        lastModuleData: FirModuleData,
        targetPlatform: TargetPlatform,
        sessionProvider: FirProjectSessionProvider,
        noinline sessionConfigurator: FirSessionConfigurator.() -> Unit,
        createSourceSession: FirSessionProducer<F>,
    ): SessionWithSources<F> =
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
            sessionProvider, sessionConfigurator, createSourceSession
        )

    inline fun <F> createSingleSession(
        files: List<F>,
        rootModuleName: Name,
        libraryList: DependencyListForCliModule,
        targetPlatform: TargetPlatform,
        sessionProvider: FirProjectSessionProvider,
        noinline sessionConfigurator: FirSessionConfigurator.() -> Unit,
        createFirSession: FirSessionProducer<F>,
    ): SessionWithSources<F> {
        val platformModuleData = FirSourceModuleData(
            rootModuleName,
            libraryList.regularDependencies,
            libraryList.dependsOnDependencies,
            libraryList.friendDependencies,
            targetPlatform,
        )

        val session = createFirSession(files, platformModuleData, sessionProvider) {
            sessionConfigurator()
            useCheckers(CliOnlyLanguageVersionSettingsCheckers)
        }
        return SessionWithSources(session, files)
    }

    inline fun <F> createSessionsForLegacyMppProject(
        files: List<F>,
        rootModuleName: Name,
        libraryList: DependencyListForCliModule,
        targetPlatform: TargetPlatform,
        sessionProvider: FirProjectSessionProvider,
        noinline sessionConfigurator: FirSessionConfigurator.() -> Unit,
        isCommonSource: (F) -> Boolean,
        createFirSession: FirSessionProducer<F>,
    ): List<SessionWithSources<F>> {
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

        val commonFiles = mutableListOf<F>()
        val platformFiles = mutableListOf<F>()
        for (file in files) {
            (if (isCommonSource(file)) commonFiles else platformFiles).add(file)
        }

        val commonSession = createFirSession(commonFiles, commonModuleData, sessionProvider, sessionConfigurator)
        val platformSession = createFirSession(platformFiles, platformModuleData, sessionProvider) {
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

    inline fun <F> createSessionsForHmppProject(
        files: List<F>,
        rootModuleName: Name,
        hmppModuleStructure: HmppCliModuleStructure,
        libraryList: DependencyListForCliModule,
        targetPlatform: TargetPlatform,
        sessionProvider: FirProjectSessionProvider,
        noinline sessionConfigurator: FirSessionConfigurator.() -> Unit,
        fileBelongsToModule: (F, String) -> Boolean,
        createFirSession: FirSessionProducer<F>,
    ): List<SessionWithSources<F>> {
        val moduleDataForHmppModule = LinkedHashMap<HmppCliModule, FirModuleData>()

        for ((index, module) in hmppModuleStructure.modules.withIndex()) {
            val dependencies = hmppModuleStructure.dependenciesMap[module]
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

        return hmppModuleStructure.modules.mapIndexed { i, module ->
            val moduleData = moduleDataForHmppModule.getValue(module)
            val sources = files.filter { fileBelongsToModule(it, module.name) }
            val session = createFirSession(sources, moduleData, sessionProvider) {
                sessionConfigurator()
                // The CLI session might contain an opt-in for an annotation that's defined in one of the modules.
                // The only module that's guaranteed to have a dependency on this module is the last one.
                if (i == hmppModuleStructure.modules.lastIndex) {
                    useCheckers(CliOnlyLanguageVersionSettingsCheckers)
                }
            }
            SessionWithSources(session, sources)
        }
    }
}

data class SessionWithSources<F>(val session: FirSession, val files: List<F>)
