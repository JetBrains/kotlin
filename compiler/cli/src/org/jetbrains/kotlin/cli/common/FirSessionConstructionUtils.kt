/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.analyzer.common.CommonPlatformAnalyzerServices
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.GroupedKtSources
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.checkers.registerExtendedCommonCheckers
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.load.kotlin.PackageAndMetadataPartProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import org.jetbrains.kotlin.resolve.konan.platform.NativePlatformAnalyzerServices
import org.jetbrains.kotlin.resolve.multiplatform.hmppModuleName
import org.jetbrains.kotlin.resolve.multiplatform.isCommonSource

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
fun <F> prepareJvmSessions(
    files: List<F>,
    configuration: CompilerConfiguration,
    projectEnvironment: AbstractProjectEnvironment,
    rootModuleName: Name,
    extensionRegistrars: List<FirExtensionRegistrar>,
    librariesScope: AbstractProjectFileSearchScope,
    libraryList: DependencyListForCliModule,
    isCommonSource: (F) -> Boolean,
    fileBelongsToModule: (F, String) -> Boolean,
    createProviderAndScopeForIncrementalCompilation: (List<F>) -> IncrementalCompilationContext?,
): List<SessionWithSources<F>> {
    val javaSourcesScope = projectEnvironment.getSearchScopeForProjectJavaSources()

    return prepareSessions(
        files, configuration, rootModuleName, JvmPlatforms.unspecifiedJvmPlatform,
        JvmPlatformAnalyzerServices, metadataCompilationMode = false, libraryList, isCommonSource, fileBelongsToModule,
        createLibrarySession = { sessionProvider ->
            FirJvmSessionFactory.createLibrarySession(
                rootModuleName,
                sessionProvider,
                libraryList.moduleDataProvider,
                projectEnvironment,
                extensionRegistrars,
                librariesScope,
                projectEnvironment.getPackagePartProvider(librariesScope),
                configuration.languageVersionSettings,
                registerExtraComponents = {},
            )
        }
    ) { moduleFiles, moduleData, sessionProvider, sessionConfigurator ->
        FirJvmSessionFactory.createModuleBasedSession(
            moduleData,
            sessionProvider,
            javaSourcesScope,
            projectEnvironment,
            createProviderAndScopeForIncrementalCompilation(moduleFiles),
            extensionRegistrars,
            configuration.languageVersionSettings,
            configuration.get(CommonConfigurationKeys.LOOKUP_TRACKER),
            configuration.get(CommonConfigurationKeys.ENUM_WHEN_TRACKER),
            needRegisterJavaElementFinder = true,
            registerExtraComponents = {},
            sessionConfigurator,
        )
    }
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
): List<SessionWithSources<F>> {
    return prepareSessions(
        files, configuration, rootModuleName, JsPlatforms.defaultJsPlatform, JsPlatformAnalyzerServices,
        metadataCompilationMode = false, libraryList, isCommonSource, fileBelongsToModule,
        createLibrarySession = { sessionProvider ->
            FirJsSessionFactory.createLibrarySession(
                rootModuleName,
                resolvedLibraries,
                sessionProvider,
                libraryList.moduleDataProvider,
                extensionRegistrars,
                configuration.languageVersionSettings,
                registerExtraComponents = {},
            )
        }
    ) { _, moduleData, sessionProvider, sessionConfigurator ->
        FirJsSessionFactory.createModuleBasedSession(
            moduleData,
            sessionProvider,
            extensionRegistrars,
            configuration.languageVersionSettings,
            null,
            registerExtraComponents = {},
            init = sessionConfigurator,
        )
    }
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
    resolvedLibraries: List<KotlinResolvedLibrary>,
    libraryList: DependencyListForCliModule,
    extensionRegistrars: List<FirExtensionRegistrar>,
    isCommonSource: (F) -> Boolean,
    fileBelongsToModule: (F, String) -> Boolean,
    registerExtraComponents: ((FirSession) -> Unit) = {},
): List<SessionWithSources<F>> {
    return prepareSessions(
        files, configuration, rootModuleName, NativePlatforms.unspecifiedNativePlatform, NativePlatformAnalyzerServices,
        metadataCompilationMode = false, libraryList, isCommonSource, fileBelongsToModule, createLibrarySession = { sessionProvider ->
            FirNativeSessionFactory.createLibrarySession(
                rootModuleName,
                resolvedLibraries,
                sessionProvider,
                libraryList.moduleDataProvider,
                extensionRegistrars,
                configuration.languageVersionSettings,
                registerExtraComponents,
            )
        }
    ) { _, moduleData, sessionProvider, sessionConfigurator ->
        FirNativeSessionFactory.createModuleBasedSession(
            moduleData,
            sessionProvider,
            extensionRegistrars,
            configuration.languageVersionSettings,
            sessionConfigurator,
            registerExtraComponents,
        )
    }
}

/**
 * Creates library session and sources session for Common platform (for metadata compilation)
 * Number of created sessions is always one, in this mode modules are compiled against compiled
 *   metadata of dependent modules
 */
fun <F> prepareCommonSessions(
    files: List<F>,
    configuration: CompilerConfiguration,
    projectEnvironment: AbstractProjectEnvironment,
    rootModuleName: Name,
    extensionRegistrars: List<FirExtensionRegistrar>,
    librariesScope: AbstractProjectFileSearchScope,
    libraryList: DependencyListForCliModule,
    resolvedLibraries: List<KotlinResolvedLibrary>,
    isCommonSource: (F) -> Boolean,
    fileBelongsToModule: (F, String) -> Boolean,
    createProviderAndScopeForIncrementalCompilation: (List<F>) -> IncrementalCompilationContext?,
): List<SessionWithSources<F>> {
    return prepareSessions(
        files, configuration, rootModuleName, CommonPlatforms.defaultCommonPlatform, CommonPlatformAnalyzerServices,
        metadataCompilationMode = true, libraryList, isCommonSource, fileBelongsToModule, createLibrarySession = { sessionProvider ->
            FirCommonSessionFactory.createLibrarySession(
                rootModuleName,
                sessionProvider,
                libraryList.moduleDataProvider,
                projectEnvironment,
                extensionRegistrars,
                librariesScope,
                resolvedLibraries,
                projectEnvironment.getPackagePartProvider(librariesScope) as PackageAndMetadataPartProvider,
                configuration.languageVersionSettings,
                registerExtraComponents = {},
            )
        }
    ) { moduleFiles, moduleData, sessionProvider, sessionConfigurator ->
        FirCommonSessionFactory.createModuleBasedSession(
            moduleData,
            sessionProvider,
            projectEnvironment,
            incrementalCompilationContext = createProviderAndScopeForIncrementalCompilation(moduleFiles),
            extensionRegistrars,
            configuration.languageVersionSettings,
            lookupTracker = configuration.get(CommonConfigurationKeys.LOOKUP_TRACKER),
            enumWhenTracker = configuration.get(CommonConfigurationKeys.ENUM_WHEN_TRACKER),
            registerExtraComponents = {},
            init = sessionConfigurator
        )
    }
}

// ---------------------------------------------------- Implementation ----------------------------------------------------

private typealias FirSessionProducer<F> = (List<F>, FirModuleData, FirProjectSessionProvider, FirSessionConfigurator.() -> Unit) -> FirSession

private inline fun <F> prepareSessions(
    files: List<F>,
    configuration: CompilerConfiguration,
    rootModuleName: Name,
    targetPlatform: TargetPlatform,
    analyzerServices: PlatformDependentAnalyzerServices,
    metadataCompilationMode: Boolean,
    libraryList: DependencyListForCliModule,
    isCommonSource: (F) -> Boolean,
    fileBelongsToModule: (F, String) -> Boolean,
    createLibrarySession: (FirProjectSessionProvider) -> FirSession,
    createSourceSession: FirSessionProducer<F>,
): List<SessionWithSources<F>> {
    val languageVersionSettings = configuration.languageVersionSettings

    val isMppEnabled = languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)
    val hmppModuleStructure = configuration.get(CommonConfigurationKeys.HMPP_MODULE_STRUCTURE)
    val sessionProvider = FirProjectSessionProvider()

    createLibrarySession(sessionProvider)
    val extendedAnalysisMode = configuration.getBoolean(CommonConfigurationKeys.USE_FIR_EXTENDED_CHECKERS)
    val sessionConfigurator: FirSessionConfigurator.() -> Unit = {
        if (extendedAnalysisMode) {
            registerExtendedCommonCheckers()
        }
    }

    return when {
        metadataCompilationMode || !isMppEnabled -> listOf(
            createSingleSession(
                files, rootModuleName, libraryList, targetPlatform, analyzerServices,
                sessionProvider, sessionConfigurator, createSourceSession
            )
        )

        hmppModuleStructure == null -> createSessionsForLegacyMppProject(
            files, rootModuleName, libraryList, targetPlatform, analyzerServices,
            sessionProvider, sessionConfigurator, isCommonSource, createSourceSession
        )

        else -> createSessionsForHmppProject(
            files, rootModuleName, hmppModuleStructure, libraryList, targetPlatform, analyzerServices,
            sessionProvider, sessionConfigurator, fileBelongsToModule, createSourceSession
        )
    }
}

private inline fun <F> createSingleSession(
    files: List<F>,
    rootModuleName: Name,
    libraryList: DependencyListForCliModule,
    targetPlatform: TargetPlatform,
    analyzerServices: PlatformDependentAnalyzerServices,
    sessionProvider: FirProjectSessionProvider,
    noinline sessionConfigurator: FirSessionConfigurator.() -> Unit,
    createFirSession: FirSessionProducer<F>,
): SessionWithSources<F> {
    val platformModuleData = FirModuleDataImpl(
        rootModuleName,
        libraryList.regularDependencies,
        libraryList.dependsOnDependencies,
        libraryList.friendsDependencies,
        targetPlatform,
        analyzerServices
    )

    val session = createFirSession(files, platformModuleData, sessionProvider, sessionConfigurator)
    return SessionWithSources(session, files)
}

private inline fun <F> createSessionsForLegacyMppProject(
    files: List<F>,
    rootModuleName: Name,
    libraryList: DependencyListForCliModule,
    targetPlatform: TargetPlatform,
    analyzerServices: PlatformDependentAnalyzerServices,
    sessionProvider: FirProjectSessionProvider,
    noinline sessionConfigurator: FirSessionConfigurator.() -> Unit,
    isCommonSource: (F) -> Boolean,
    createFirSession: FirSessionProducer<F>,
): List<SessionWithSources<F>> {
    val commonModuleData = FirModuleDataImpl(
        Name.identifier("${rootModuleName.asString()}-common"),
        libraryList.regularDependencies,
        listOf(),
        libraryList.friendsDependencies,
        targetPlatform,
        analyzerServices
    )

    val platformModuleData = FirModuleDataImpl(
        rootModuleName,
        libraryList.regularDependencies,
        listOf(commonModuleData),
        libraryList.friendsDependencies,
        targetPlatform,
        analyzerServices
    )

    val commonFiles = mutableListOf<F>()
    val platformFiles = mutableListOf<F>()
    for (file in files) {
        (if (isCommonSource(file)) commonFiles else platformFiles).add(file)
    }

    val commonSession = createFirSession(commonFiles, commonModuleData, sessionProvider, sessionConfigurator)
    val platformSession = createFirSession(platformFiles, platformModuleData, sessionProvider, sessionConfigurator)

    return listOf(
        SessionWithSources(commonSession, commonFiles),
        SessionWithSources(platformSession, platformFiles)
    )
}

private inline fun <F> createSessionsForHmppProject(
    files: List<F>,
    rootModuleName: Name,
    hmppModuleStructure: HmppCliModuleStructure,
    libraryList: DependencyListForCliModule,
    targetPlatform: TargetPlatform,
    analyzerServices: PlatformDependentAnalyzerServices,
    sessionProvider: FirProjectSessionProvider,
    noinline sessionConfigurator: FirSessionConfigurator.() -> Unit,
    fileBelongsToModule: (F, String) -> Boolean,
    createFirSession: FirSessionProducer<F>,
): List<SessionWithSources<F>> {
    val moduleDataForHmppModule = LinkedHashMap<HmppCliModule, FirModuleData>()

    for (module in hmppModuleStructure.modules) {
        val dependencies = hmppModuleStructure.dependenciesMap[module]
            ?.map { moduleDataForHmppModule.getValue(it) }
            .orEmpty()
        val moduleData = FirModuleDataImpl(
            rootModuleName,
            libraryList.regularDependencies,
            dependsOnDependencies = dependencies,
            libraryList.friendsDependencies,
            targetPlatform,
            analyzerServices
        )
        moduleDataForHmppModule[module] = moduleData
    }

    return hmppModuleStructure.modules.map { module ->
        val moduleData = moduleDataForHmppModule.getValue(module)
        val sources = files.filter { fileBelongsToModule(it, module.name) }
        val session = createFirSession(sources, moduleData, sessionProvider, sessionConfigurator)
        SessionWithSources(session, sources)
    }
}

data class SessionWithSources<F>(val session: FirSession, val files: List<F>)
