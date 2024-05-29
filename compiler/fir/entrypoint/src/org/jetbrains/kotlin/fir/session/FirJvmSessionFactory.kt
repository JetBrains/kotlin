/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.checkers.registerJvmCheckers
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirJvmTargetProvider
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.OptionalAnnotationClassesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCloneableSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirStdlibBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.FirSessionFactoryHelper.registerDefaultComponents
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker
import org.jetbrains.kotlin.incremental.components.ImportTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.addToStdlib.runIf

object FirJvmSessionFactory : FirAbstractSessionFactory() {
    fun createLibrarySession(
        mainModuleName: Name,
        sessionProvider: FirProjectSessionProvider,
        moduleDataProvider: ModuleDataProvider,
        projectEnvironment: AbstractProjectEnvironment,
        extensionRegistrars: List<FirExtensionRegistrar>,
        scope: AbstractProjectFileSearchScope,
        packagePartProvider: PackagePartProvider,
        languageVersionSettings: LanguageVersionSettings,
        predefinedJavaComponents: FirSharableJavaComponents?,
        registerExtraComponents: ((FirSession) -> Unit),
    ): FirSession {
        return createLibrarySession(
            mainModuleName,
            sessionProvider,
            moduleDataProvider,
            languageVersionSettings,
            extensionRegistrars,
            registerExtraComponents = {
                it.registerDefaultComponents()
                it.registerJavaComponents(projectEnvironment.getJavaModuleResolver(), predefinedJavaComponents)
                registerExtraComponents(it)
            },
            createKotlinScopeProvider = { FirKotlinScopeProvider(::wrapScopeWithJvmMapped) },
            createProviders = { session, builtinsModuleData, kotlinScopeProvider, syntheticFunctionInterfaceProvider ->
                listOfNotNull(
                    JvmClassFileBasedSymbolProvider(
                        session,
                        moduleDataProvider,
                        kotlinScopeProvider,
                        packagePartProvider,
                        projectEnvironment.getKotlinClassFinder(scope),
                        projectEnvironment.getFirJavaFacade(session, moduleDataProvider.allModuleData.last(), scope)
                    ),
                    FirBuiltinSymbolProvider(session, builtinsModuleData, kotlinScopeProvider),
                    syntheticFunctionInterfaceProvider,
                    FirCloneableSymbolProvider(session, builtinsModuleData, kotlinScopeProvider),
                    OptionalAnnotationClassesProvider(
                        session,
                        moduleDataProvider,
                        kotlinScopeProvider,
                        packagePartProvider
                    )
                )
            }
        )
    }

    @OptIn(SessionConfiguration::class)
    fun createModuleBasedSession(
        moduleData: FirModuleData,
        sessionProvider: FirProjectSessionProvider,
        javaSourcesScope: AbstractProjectFileSearchScope,
        projectEnvironment: AbstractProjectEnvironment,
        createIncrementalCompilationSymbolProviders: (FirSession) -> FirJvmIncrementalCompilationSymbolProviders?,
        extensionRegistrars: List<FirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings,
        jvmTarget: JvmTarget,
        lookupTracker: LookupTracker?,
        enumWhenTracker: EnumWhenTracker?,
        importTracker: ImportTracker?,
        predefinedJavaComponents: FirSharableJavaComponents?,
        needRegisterJavaElementFinder: Boolean,
        registerExtraComponents: ((FirSession) -> Unit),
        init: FirSessionConfigurator.() -> Unit,
    ): FirSession {
        return createModuleBasedSession(
            moduleData,
            sessionProvider,
            extensionRegistrars,
            languageVersionSettings,
            lookupTracker,
            enumWhenTracker,
            importTracker,
            init,
            registerExtraComponents = {
                it.registerDefaultComponents()
                it.registerJavaComponents(projectEnvironment.getJavaModuleResolver(), predefinedJavaComponents)
                it.register(FirJvmTargetProvider::class, FirJvmTargetProvider(jvmTarget))
                registerExtraComponents(it)
            },
            registerExtraCheckers = { it.registerJvmCheckers() },
            createKotlinScopeProvider = {
                if (languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation) && moduleData.isCommon) {
                    FirKotlinScopeProvider()
                } else {
                    FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
                }
            },
            createProviders = { session, kotlinScopeProvider, symbolProvider, generatedSymbolsProvider, dependencies ->
                val javaSymbolProvider =
                    JavaSymbolProvider(session, projectEnvironment.getFirJavaFacade(session, moduleData, javaSourcesScope))
                session.register(JavaSymbolProvider::class, javaSymbolProvider)

                val incrementalCompilationSymbolProviders = createIncrementalCompilationSymbolProviders(session)

                listOfNotNull(
                    symbolProvider,
                    *(incrementalCompilationSymbolProviders?.previousFirSessionsSymbolProviders?.toTypedArray() ?: emptyArray()),
                    incrementalCompilationSymbolProviders?.symbolProviderForBinariesFromIncrementalCompilation,
                    generatedSymbolsProvider,
                    javaSymbolProvider,
                    createJvmActualizingBuiltinSymbolProviderIfNeeded(languageVersionSettings, moduleData, dependencies),
                    FirStdlibBuiltinSyntheticFunctionInterfaceProvider.initializeIfNeeded(
                        session,
                        moduleData,
                        kotlinScopeProvider,
                    ),
                    *dependencies.toTypedArray(),
                    incrementalCompilationSymbolProviders?.optionalAnnotationClassesProviderForBinariesFromIncrementalCompilation,
                )
            }
        ).also {
            if (needRegisterJavaElementFinder) {
                projectEnvironment.registerAsJavaElementFinder(it)
            }
        }
    }

    private fun createJvmActualizingBuiltinSymbolProviderIfNeeded(
        languageVersionSettings: LanguageVersionSettings,
        moduleData: FirModuleData,
        dependencies: List<FirSymbolProvider>,
    ): FirJvmActualizingBuiltinSymbolProvider? =
        runIf(languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation) && !moduleData.isCommon) {
            val dependentSymbolProviders = dependencies.filter { it.session.kind == FirSession.Kind.Source }
            val builtinSymbolProvider = dependencies.firstIsInstance<FirBuiltinSymbolProvider>()
            FirJvmActualizingBuiltinSymbolProvider(dependentSymbolProviders, builtinSymbolProvider)
        }
}

