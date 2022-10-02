/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.checkers.registerJvmCheckers
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.OptionalAnnotationClassesProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCloneableSymbolProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.Name

object FirJvmSessionFactory : FirAbstractSessionFactory() {
    fun createLibrarySession(
        mainModuleName: Name,
        sessionProvider: FirProjectSessionProvider,
        dependencyList: DependencyListForCliModule,
        projectEnvironment: AbstractProjectEnvironment,
        scope: AbstractProjectFileSearchScope,
        packagePartProvider: PackagePartProvider,
        languageVersionSettings: LanguageVersionSettings,
    ): FirSession {
        return createLibrarySession(
            mainModuleName,
            sessionProvider,
            dependencyList.moduleDataProvider,
            languageVersionSettings,
            registerExtraComponents = { it.registerCommonJavaComponents(projectEnvironment.getJavaModuleResolver()) },
            createKotlinScopeProvider = { FirKotlinScopeProvider(::wrapScopeWithJvmMapped) },
            createProviders = { session, builtinsModuleData, kotlinScopeProvider ->
                listOf(
                    JvmClassFileBasedSymbolProvider(
                        session,
                        dependencyList.moduleDataProvider,
                        kotlinScopeProvider,
                        packagePartProvider,
                        projectEnvironment.getKotlinClassFinder(scope),
                        projectEnvironment.getFirJavaFacade(session, dependencyList.moduleDataProvider.allModuleData.last(), scope)
                    ),
                    FirBuiltinSymbolProvider(session, builtinsModuleData, kotlinScopeProvider),
                    FirCloneableSymbolProvider(session, builtinsModuleData, kotlinScopeProvider),
                    OptionalAnnotationClassesProvider(session, dependencyList.moduleDataProvider, kotlinScopeProvider, packagePartProvider)
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
        incrementalCompilationContext: IncrementalCompilationContext?,
        extensionRegistrars: List<FirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
        lookupTracker: LookupTracker? = null,
        enumWhenTracker: EnumWhenTracker? = null,
        needRegisterJavaElementFinder: Boolean,
        init: FirSessionConfigurator.() -> Unit = {}
    ): FirSession {
        return createModuleBasedSession(
            moduleData,
            sessionProvider,
            extensionRegistrars,
            languageVersionSettings,
            lookupTracker,
            enumWhenTracker,
            init,
            registerExtraComponents = {
                it.registerCommonJavaComponents(projectEnvironment.getJavaModuleResolver())
                it.registerJavaSpecificResolveComponents()
            },
            registerExtraCheckers = { it.registerJvmCheckers() },
            createKotlinScopeProvider = { FirKotlinScopeProvider(::wrapScopeWithJvmMapped) },
            createProviders = { session, kotlinScopeProvider, symbolProvider, generatedSymbolsProvider, dependenciesSymbolProvider ->
                var symbolProviderForBinariesFromIncrementalCompilation: JvmClassFileBasedSymbolProvider? = null
                var optionalAnnotationClassesProviderForBinariesFromIncrementalCompilation: OptionalAnnotationClassesProvider? = null
                incrementalCompilationContext?.let {
                    if (it.precompiledBinariesPackagePartProvider != null && it.precompiledBinariesFileScope != null) {
                        val moduleDataProvider = SingleModuleDataProvider(moduleData)
                        symbolProviderForBinariesFromIncrementalCompilation =
                            JvmClassFileBasedSymbolProvider(
                                session,
                                moduleDataProvider,
                                kotlinScopeProvider,
                                it.precompiledBinariesPackagePartProvider,
                                projectEnvironment.getKotlinClassFinder(it.precompiledBinariesFileScope),
                                projectEnvironment.getFirJavaFacade(session, moduleData, it.precompiledBinariesFileScope),
                                defaultDeserializationOrigin = FirDeclarationOrigin.Precompiled
                            )
                        optionalAnnotationClassesProviderForBinariesFromIncrementalCompilation =
                            OptionalAnnotationClassesProvider(
                                session,
                                moduleDataProvider,
                                kotlinScopeProvider,
                                it.precompiledBinariesPackagePartProvider,
                                defaultDeserializationOrigin = FirDeclarationOrigin.Precompiled
                            )
                    }
                }

                val javaSymbolProvider = JavaSymbolProvider(session, projectEnvironment.getFirJavaFacade(session, moduleData, javaSourcesScope))
                session.register(JavaSymbolProvider::class, javaSymbolProvider)

                listOfNotNull(
                    symbolProvider,
                    *(incrementalCompilationContext?.previousFirSessionsSymbolProviders?.toTypedArray() ?: emptyArray()),
                    symbolProviderForBinariesFromIncrementalCompilation,
                    generatedSymbolsProvider,
                    javaSymbolProvider,
                    dependenciesSymbolProvider,
                    optionalAnnotationClassesProviderForBinariesFromIncrementalCompilation,
                )
            }
        ).also {
            if (needRegisterJavaElementFinder) {
                projectEnvironment.registerAsJavaElementFinder(it)
            }
        }
    }
}
