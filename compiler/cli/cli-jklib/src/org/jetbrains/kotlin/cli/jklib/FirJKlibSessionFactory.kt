/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jklib

import org.jetbrains.kotlin.cli.common.SessionConstructionUtils
import org.jetbrains.kotlin.cli.common.SessionWithSources
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.checkers.registerJvmCheckers
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirJvmTargetProvider
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.FirJvmClasspathBuiltinSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.OptionalAnnotationClassesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCloneableSymbolProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

@OptIn(SessionConfiguration::class)
object FirJKlibSessionFactory : FirAbstractSessionFactory<FirJKlibSessionFactory.Context>() {
    override fun createPlatformSpecificSharedProviders(
        session: FirSession,
        moduleData: FirModuleData,
        scopeProvider: FirKotlinScopeProvider,
        context: Context,
    ): List<FirSymbolProvider> {
        return listOf(
            FirCloneableSymbolProvider(session, moduleData, scopeProvider),
            OptionalAnnotationClassesProvider(
                session,
                SingleModuleDataProvider(moduleData),
                scopeProvider,
                context.packagePartProvider
            ),
        )
    }

    // ==================================== Shared library session ====================================

    /**
     * See documentation to [FirAbstractSessionFactory.createSharedLibrarySession]
     */
    fun createSharedLibrarySession(
        mainModuleName: Name,
        projectEnvironment: AbstractProjectEnvironment,
        extensionRegistrars: List<FirExtensionRegistrar>,
        packagePartProvider: PackagePartProvider,
        languageVersionSettings: LanguageVersionSettings,
        predefinedJavaComponents: FirSharableJavaComponents?,
    ): FirSession {
        val context = Context(predefinedJavaComponents, projectEnvironment, packagePartProvider)
        return createSharedLibrarySession(
            mainModuleName,
            context,
            languageVersionSettings,
            extensionRegistrars
        )
    }

    // ==================================== Library session ====================================

    /**
     * See documentation to [FirAbstractSessionFactory.createLibrarySession]
     */
    fun createLibrarySession(
        resolvedLibraries: List<KotlinLibrary>,
        sharedLibrarySession: FirSession,
        moduleDataProvider: ModuleDataProvider,
        projectEnvironment: AbstractProjectEnvironment,
        extensionRegistrars: List<FirExtensionRegistrar>,
        scope: AbstractProjectFileSearchScope,
        packagePartProvider: PackagePartProvider,
        languageVersionSettings: LanguageVersionSettings,
        predefinedJavaComponents: FirSharableJavaComponents?,
    ): FirSession {
        val kotlinClassFinder = projectEnvironment.getKotlinClassFinder(scope)
        val context = Context(predefinedJavaComponents, projectEnvironment, packagePartProvider)
        return createLibrarySession(
            context,
            sharedLibrarySession,
            moduleDataProvider,
            languageVersionSettings,
            extensionRegistrars,
            createSeparateSharedProvidersInHmppCompilation = false,
            createProviders = { session, kotlinScopeProvider ->
                listOf(
                    KlibBasedSymbolProvider(
                        session, moduleDataProvider, kotlinScopeProvider, resolvedLibraries,
                    ),
                    JvmClassFileBasedSymbolProvider(
                        session,
                        moduleDataProvider,
                        kotlinScopeProvider,
                        packagePartProvider,
                        kotlinClassFinder,
                        projectEnvironment.getFirJavaFacade(session, moduleDataProvider.allModuleData.last(), scope)
                    ),
                )
            }
        )
    }

    override fun createKotlinScopeProviderForLibrarySession(): FirKotlinScopeProvider {
        return FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
    }

    override fun FirSession.registerLibrarySessionComponents(c: Context) {
        registerJavaComponents(c.projectEnvironment.getJavaModuleResolver(), c.predefinedJavaComponents)
    }

    // ==================================== Platform session ====================================

    /**
     * See documentation to [FirAbstractSessionFactory.createSourceSession]
     */
    fun createSourceSession(
        moduleData: FirModuleData,
        javaSourcesScope: AbstractProjectFileSearchScope,
        projectEnvironment: AbstractProjectEnvironment,
        createIncrementalCompilationSymbolProviders: (FirSession) -> FirJvmIncrementalCompilationSymbolProviders?,
        extensionRegistrars: List<FirExtensionRegistrar>,
        configuration: CompilerConfiguration,
        predefinedJavaComponents: FirSharableJavaComponents?,
        needRegisterJavaElementFinder: Boolean,
        packagePartProvider: PackagePartProvider, // TODO create a separate SourceContext to not pass this argument here
        isForLeafHmppModule: Boolean,
        init: FirSessionConfigurator.() -> Unit,
    ): FirSession {
        val context = Context(predefinedJavaComponents, projectEnvironment, packagePartProvider)
        return createSourceSession(
            moduleData,
            context = context,
            extensionRegistrars,
            configuration,
            isForLeafHmppModule = isForLeafHmppModule,
            init,
            createProviders = { session, kotlinScopeProvider, symbolProvider, generatedSymbolsProvider ->
                val javaSymbolProvider =
                    JavaSymbolProvider(session, projectEnvironment.getFirJavaFacade(session, moduleData, javaSourcesScope))
                session.register(JavaSymbolProvider::class, javaSymbolProvider)

                val incrementalCompilationSymbolProviders = createIncrementalCompilationSymbolProviders(session)

                val providers = listOfNotNull(
                    symbolProvider,
                    generatedSymbolsProvider,
                    *(incrementalCompilationSymbolProviders?.previousFirSessionsSymbolProviders?.toTypedArray() ?: emptyArray()),
                    incrementalCompilationSymbolProviders?.symbolProviderForBinariesFromIncrementalCompilation,
                    javaSymbolProvider,
                    initializeForStdlibIfNeeded(projectEnvironment, session, kotlinScopeProvider),
                )
                SourceProviders(
                    providers,
                    incrementalCompilationSymbolProviders?.optionalAnnotationClassesProviderForBinariesFromIncrementalCompilation
                )
            }
        ).also {
            if (needRegisterJavaElementFinder) {
                projectEnvironment.registerAsJavaElementFinder(it)
            }
        }
    }

    override fun createKotlinScopeProviderForSourceSession(
        moduleData: FirModuleData,
        languageVersionSettings: LanguageVersionSettings,
    ): FirKotlinScopeProvider {
        if (languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation) && moduleData.isCommon) return FirKotlinScopeProvider()
        return FirKotlinScopeProvider { klass, declaredScope, useSiteSession, scopeSession, memberRequiredPhase ->
            wrapScopeWithJvmMapped(
                klass,
                declaredScope,
                useSiteSession,
                scopeSession,
                memberRequiredPhase,
                filterOutJvmPlatformDeclarations = !languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation)
            )
        }
    }

    override fun FirSessionConfigurator.registerPlatformCheckers() {
        registerJvmCheckers()
    }

    override fun FirSessionConfigurator.registerExtraPlatformCheckers() {
    }

    override fun FirSession.registerSourceSessionComponents(c: Context) {
        registerLibrarySessionComponents(c)
        register(FirJvmTargetProvider::class, FirJvmTargetProvider(JvmTarget.Companion.DEFAULT))
    }

    override val requiresSpecialSetupOfSourceProvidersInHmppCompilation: Boolean
        get() = false
    override val isFactoryForMetadataCompilation: Boolean
        get() = false

    // ==================================== Common parts ====================================

    // ==================================== Utilities ====================================

    class Context(
        val predefinedJavaComponents: FirSharableJavaComponents?,
        val projectEnvironment: AbstractProjectEnvironment,
        val packagePartProvider: PackagePartProvider,
    )

    private fun initializeForStdlibIfNeeded(
        projectEnvironment: AbstractProjectEnvironment,
        session: FirSession,
        kotlinScopeProvider: FirKotlinScopeProvider,
    ): FirSymbolProvider? {
        return runIf(
            session.languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation) &&
                    !session.moduleData.isCommon
                    && session.moduleData.dependsOnDependencies.isEmpty()
        ) {
            val kotlinClassFinder = projectEnvironment.getKotlinClassFinder(projectEnvironment.getSearchScopeForProjectLibraries())
            FirJvmClasspathBuiltinSymbolProvider(
                session,
                session.moduleData,
                kotlinScopeProvider
            ) { kotlinClassFinder.findBuiltInsData(it) }
        }
    }
}

/**
 * Creates library session and sources session for klib compilation Number of created session
 * depends on mode of MPP:
 * - disabled
 * - legacy (one platform and one common module)
 * - HMPP (multiple number of modules)
 */
internal fun <F> prepareJKlibSessions(
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
        files,
        configuration,
        rootModuleName,
        NativePlatforms.unspecifiedNativePlatform,
        metadataCompilationMode,
        libraryList,
        extensionRegistrars,
        isCommonSource,
        isScript = { false },
        fileBelongsToModule,
        createSharedLibrarySession = {
            FirJKlibSessionFactory.createSharedLibrarySession(
                rootModuleName,
                projectEnvironment,
                extensionRegistrars,
                packagePartProviderForLibraries,
                configuration.languageVersionSettings,
                predefinedJavaComponents = predefinedJavaComponents,
            )
        },
        createLibrarySession = { sharedLibrarySession ->
            FirJKlibSessionFactory.createLibrarySession(
                resolvedLibraries.map { it.library },
                sharedLibrarySession,
                libraryList.moduleDataProvider,
                projectEnvironment,
                extensionRegistrars,
                librariesScope,
                packagePartProviderForLibraries,
                configuration.languageVersionSettings,
                predefinedJavaComponents = predefinedJavaComponents,
            )
        },
        createSourceSession = { _, moduleData, isForLeafHmppModule, sessionConfigurator ->
            FirJKlibSessionFactory.createSourceSession(
                moduleData = moduleData,
                javaSourcesScope = projectEnvironment.getSearchScopeForProjectJavaSources(),
                projectEnvironment = projectEnvironment,
                createIncrementalCompilationSymbolProviders = { null },
                extensionRegistrars = extensionRegistrars,
                configuration = configuration,
                predefinedJavaComponents = predefinedJavaComponents,
                needRegisterJavaElementFinder = true,
                packagePartProvider = packagePartProviderForLibraries,
                init = sessionConfigurator,
                isForLeafHmppModule = isForLeafHmppModule,
            )
        },
        createMetadataSessionFactoryContextForHmppCommonLibrarySession = {
            AbstractFirMetadataSessionFactory.Context(
                createJvmContext = { shouldNotBeCalled() },
                createJsContext = { shouldNotBeCalled() }
            )
        }
    )
}