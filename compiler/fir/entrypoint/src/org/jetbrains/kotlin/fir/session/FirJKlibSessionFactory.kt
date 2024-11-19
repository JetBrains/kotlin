/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.checkers.registerJvmCheckers
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.OptionalAnnotationClassesProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCloneableSymbolProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.name.Name

@OptIn(SessionConfiguration::class)
object FirJKlibSessionFactory :
    FirAbstractSessionFactory<FirJKlibSessionFactory.Context, FirJKlibSessionFactory.Context>() {

    // ==================================== Library session ====================================

    fun createLibrarySession(
        mainModuleName: Name,
        resolvedLibraries: List<KotlinResolvedLibrary>,
        sessionProvider: FirProjectSessionProvider,
        moduleDataProvider: ModuleDataProvider,
        projectEnvironment: AbstractProjectEnvironment,
        extensionRegistrars: List<FirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings,
        predefinedJavaComponents: FirSharableJavaComponents,
    ): FirSession {
        return createLibrarySession(
            mainModuleName,
            context = Context(projectEnvironment, predefinedJavaComponents),
            sessionProvider,
            moduleDataProvider,
            languageVersionSettings,
            extensionRegistrars,
            createProviders = {
                    session,
                    builtinsModuleData,
                    kotlinScopeProvider,
                    syntheticFunctionInterfaceProvider ->
                val resolvedKotlinLibraries = resolvedLibraries.map { it.library }
                val scope = projectEnvironment.getSearchScopeForProjectLibraries()
                val packagePartProvider = projectEnvironment.getPackagePartProvider(scope)
                val kotlinClassFinder = projectEnvironment.getKotlinClassFinder(scope)

                listOfNotNull(
                    KlibBasedSymbolProvider(
                        session,
                        moduleDataProvider,
                        kotlinScopeProvider,
                        resolvedKotlinLibraries,
                    ),
                    JvmClassFileBasedSymbolProvider(
                        session,
                        moduleDataProvider,
                        kotlinScopeProvider,
                        packagePartProvider,
                        kotlinClassFinder,
                        projectEnvironment.getFirJavaFacade(
                            session,
                            moduleDataProvider.allModuleData.last(),
                            scope,
                        ),
                    ),
                    FirBuiltinSyntheticFunctionInterfaceProvider(
                        session,
                        builtinsModuleData,
                        kotlinScopeProvider,
                    ),
                    syntheticFunctionInterfaceProvider,
                    FirCloneableSymbolProvider(session, builtinsModuleData, kotlinScopeProvider),
                    OptionalAnnotationClassesProvider(
                        session,
                        moduleDataProvider,
                        kotlinScopeProvider,
                        packagePartProvider,
                    ),
                )
            },
        )
    }

    override fun createKotlinScopeProviderForLibrarySession(): FirKotlinScopeProvider {
        return FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
    }

    override fun FirSession.registerLibrarySessionComponents(c: Context) {
        registerComponents()
        registerJavaComponents(c.projectEnvironment.getJavaModuleResolver(), c.predefinedJavaComponents)
    }

    // ==================================== Platform session ====================================

    fun createModuleBasedSession(
        moduleData: FirModuleData,
        sessionProvider: FirProjectSessionProvider,
        projectEnvironment: AbstractProjectEnvironment,
        extensionRegistrars: List<FirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings,
        init: FirSessionConfigurator.() -> Unit,
        predefinedJavaComponents: FirSharableJavaComponents,
    ): FirSession {
        return createModuleBasedSession(
            moduleData,
            context = Context(projectEnvironment, predefinedJavaComponents),
            sessionProvider,
            extensionRegistrars,
            languageVersionSettings,
            lookupTracker = null,
            enumWhenTracker = null,
            importTracker = null,
            init,
            createProviders = {
                    session,
                    kotlinScopeProvider,
                    symbolProvider,
                    generatedSymbolsProvider,
                    dependencies ->
                val scope = projectEnvironment.getSearchScopeForProjectLibraries()
                val javaSymbolProvider =
                    JavaSymbolProvider(
                        session,
                        projectEnvironment.getFirJavaFacade(session, moduleData, scope),
                    )
                session.register(JavaSymbolProvider::class, javaSymbolProvider)
                listOfNotNull(
                    javaSymbolProvider,
                    symbolProvider,
                    generatedSymbolsProvider,
                    *dependencies.toTypedArray(),
                )
            },
        )
            .also { projectEnvironment.registerAsJavaElementFinder(it) }
    }

    override fun createKotlinScopeProviderForSourceSession(
        moduleData: FirModuleData,
        languageVersionSettings: LanguageVersionSettings,
    ): FirKotlinScopeProvider {
        if (languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation) && moduleData.isCommon)
            return FirKotlinScopeProvider()
        return FirKotlinScopeProvider {
                klass,
                declaredScope,
                useSiteSession,
                scopeSession,
                memberRequiredPhase ->
            wrapScopeWithJvmMapped(
                klass,
                declaredScope,
                useSiteSession,
                scopeSession,
                memberRequiredPhase,
                filterOutJvmPlatformDeclarations =
                    !languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation),
            )
        }
    }

    override fun FirSessionConfigurator.registerPlatformCheckers(c: Context) {
        registerJvmCheckers()
    }

    override fun FirSession.registerSourceSessionComponents(c: Context) {
        registerComponents()
        registerJavaComponents(c.projectEnvironment.getJavaModuleResolver(), c.predefinedJavaComponents)
    }

    // ==================================== Common parts ====================================

    private fun FirSession.registerComponents() {
        registerDefaultComponents()
    }

    // ==================================== Utilities ====================================

    class Context(
        val projectEnvironment: AbstractProjectEnvironment,
        val predefinedJavaComponents: FirSharableJavaComponents,
    )
}
