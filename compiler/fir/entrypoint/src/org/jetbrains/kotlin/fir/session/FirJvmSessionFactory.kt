/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.checkers.registerJvmCheckers
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirJvmTargetProvider
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.FirJvmBuiltinsSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.FirJvmClasspathBuiltinSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.OptionalAnnotationClassesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCloneableSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirFallbackBuiltinSymbolProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.runUnless

@OptIn(SessionConfiguration::class)
object FirJvmSessionFactory : FirAbstractSessionFactory<FirJvmSessionFactory.Context>() {

    // ==================================== Shared library session ====================================

    /**
     * See documentation to [FirAbstractSessionFactory.createSharedLibrarySession]
     */
    fun createSharedLibrarySession(
        mainModuleName: Name,
        extensionRegistrars: List<FirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings,
        context: Context,
    ): FirSession {
        return createSharedLibrarySession(
            mainModuleName,
            context,
            languageVersionSettings,
            extensionRegistrars
        )
    }

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
                context.packagePartProviderForLibraries,
            )
        )
    }

    // ==================================== Library session ====================================

    /**
     * See documentation to [FirAbstractSessionFactory.createLibrarySession]
     */
    fun createLibrarySession(
        sharedLibrarySession: FirSession,
        moduleDataProvider: ModuleDataProvider,
        extensionRegistrars: List<FirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings,
        context: Context,
    ): FirSession {
        return createLibrarySession(
            context,
            sharedLibrarySession,
            moduleDataProvider,
            languageVersionSettings,
            extensionRegistrars,
            createSeparateSharedProvidersInHmppCompilation = true,
            createProviders = { session, kotlinScopeProvider ->
                val projectEnvironment = context.projectEnvironment
                val moduleData = moduleDataProvider.allModuleData.last()
                val searchScope = moduleDataProvider.getModuleDataPaths(moduleData)?.let { paths ->
                    projectEnvironment.getSearchScopeByClassPath(paths)
                } ?: context.librariesScope
                val kotlinClassFinder = projectEnvironment.getKotlinClassFinder(searchScope)
                listOfNotNull(
                    JvmClassFileBasedSymbolProvider(
                        session,
                        moduleDataProvider,
                        kotlinScopeProvider,
                        context.packagePartProviderForLibraries,
                        kotlinClassFinder,
                        projectEnvironment.getFirJavaFacade(session, moduleData, context.librariesScope)
                    ),
                    runUnless(languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation)) {
                        initializeBuiltinsProvider(
                            session,
                            moduleData,
                            kotlinScopeProvider,
                            kotlinClassFinder
                        )
                    }
                )
            }
        )
    }

    override fun createKotlinScopeProviderForLibrarySession(): FirKotlinScopeProvider {
        return FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
    }

    override fun FirSession.registerLibrarySessionComponents(c: Context) {
        registerJavaComponents(
            javaModuleResolver = c.projectEnvironment.getJavaModuleResolver(),
            predefinedComponents = c.predefinedJavaComponents,
            registerJvmDeserializationExtension = c.registerJvmDeserializationExtension,
        )
    }

    // ==================================== Platform session ====================================

    /**
     * See documentation to [FirAbstractSessionFactory.createSourceSession]
     */
    fun createSourceSession(
        moduleData: FirModuleData,
        javaSourcesScope: AbstractProjectFileSearchScope,
        createIncrementalCompilationSymbolProviders: (FirSession) -> FirJvmIncrementalCompilationSymbolProviders?,
        extensionRegistrars: List<FirExtensionRegistrar>,
        configuration: CompilerConfiguration,
        context: Context,
        needRegisterJavaElementFinder: Boolean,
        isForLeafHmppModule: Boolean,
        init: FirSessionConfigurator.() -> Unit,
    ): FirSession {
        val projectEnvironment = context.projectEnvironment
        return createSourceSession(
            moduleData,
            context = context,
            extensionRegistrars,
            configuration,
            isForLeafHmppModule,
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
        register(FirJvmTargetProvider::class, FirJvmTargetProvider(c.jvmTarget))
    }

    override val requiresSpecialSetupOfSourceProvidersInHmppCompilation: Boolean
        get() = true

    override val isFactoryForMetadataCompilation: Boolean
        get() = false

    // ==================================== Common parts ====================================

    // ==================================== Utilities ====================================

    class Context(
        val jvmTarget: JvmTarget,
        val projectEnvironment: AbstractProjectEnvironment,
        val librariesScope: AbstractProjectFileSearchScope,
        val registerJvmDeserializationExtension: Boolean
    ) {
        constructor(
            configuration: CompilerConfiguration,
            projectEnvironment: AbstractProjectEnvironment,
            librariesScope: AbstractProjectFileSearchScope,
            registerJvmDeserializationExtension: Boolean = true,
        ) : this(
            jvmTarget = configuration.jvmTarget ?: JvmTarget.DEFAULT,
            projectEnvironment,
            librariesScope,
            registerJvmDeserializationExtension = registerJvmDeserializationExtension,
        )

        val packagePartProviderForLibraries: PackagePartProvider = projectEnvironment.getPackagePartProvider(librariesScope)

        val predefinedJavaComponents: FirSharableJavaComponents = FirSharableJavaComponents(firCachesFactoryForCliMode)
    }

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

    private fun initializeBuiltinsProvider(
        session: FirSession,
        builtinsModuleData: FirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
        kotlinClassFinder: KotlinClassFinder,
    ): FirJvmBuiltinsSymbolProvider = FirJvmBuiltinsSymbolProvider(
        session,
        FirFallbackBuiltinSymbolProvider(session, builtinsModuleData, kotlinScopeProvider)
    ) { kotlinClassFinder.findBuiltInsData(it) }
}
