/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.analyzer.common.CommonDefaultImportsProvider
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCloneableSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirFallbackBuiltinSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirDefaultImportsProviderHolder
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.load.kotlin.PackageAndMetadataPartProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.KotlinMetadataFinder
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.runUnless
@OptIn(SessionConfiguration::class)
abstract class AbstractFirMetadataSessionFactory : FirAbstractSessionFactory<Nothing?, Nothing?>() {
    // ==================================== Shared library session ====================================

    /**
     * See documentation to [FirAbstractSessionFactory.createSharedLibrarySession]
     */
    fun createSharedLibrarySession(
        mainModuleName: Name,
        languageVersionSettings: LanguageVersionSettings,
        extensionRegistrars: List<FirExtensionRegistrar>,
    ): FirSession {
        return createSharedLibrarySession(
            mainModuleName,
            context = null,
            languageVersionSettings,
            extensionRegistrars
        )
    }

    // ==================================== Library session ====================================

    protected abstract val createSeparateSharedProvidersInHmppCompilation: Boolean

    /**
     * See documentation to [FirAbstractSessionFactory.createLibrarySession]
     */
    fun createLibrarySession(
        sharedLibrarySession: FirSession,
        moduleDataProvider: ModuleDataProvider,
        extensionRegistrars: List<FirExtensionRegistrar>,
        jarMetadataProviderComponents: JarMetadataProviderComponents?,
        resolvedKLibs: List<KotlinLibrary>,
        languageVersionSettings: LanguageVersionSettings,
    ): FirSession {
        return createLibrarySession(
            context = null,
            sharedLibrarySession,
            moduleDataProvider,
            languageVersionSettings,
            extensionRegistrars,
            createSeparateSharedProvidersInHmppCompilation,
            createProviders = { session, kotlinScopeProvider ->
                listOfNotNull(
                    jarMetadataProviderComponents?.let { (packageAndMetadataPartProvider, librariesScope, projectEnvironment) ->
                        MetadataSymbolProvider(
                            session,
                            moduleDataProvider,
                            kotlinScopeProvider,
                            packageAndMetadataPartProvider,
                            projectEnvironment.getKotlinClassFinder(librariesScope)
                        )
                    },
                    runIf(resolvedKLibs.isNotEmpty()) {
                        KlibBasedSymbolProvider(
                            session,
                            moduleDataProvider,
                            kotlinScopeProvider,
                            resolvedKLibs
                        )
                    },
                )
            }
        )
    }

    data class JarMetadataProviderComponents(
        val packageAndMetadataPartProvider: PackageAndMetadataPartProvider,
        val librariesScope: AbstractProjectFileSearchScope,
        val projectEnvironment: AbstractProjectEnvironment
    )

    override fun createKotlinScopeProviderForLibrarySession(): FirKotlinScopeProvider {
        return FirKotlinScopeProvider()
    }

    override fun FirSession.registerLibrarySessionComponents(c: Nothing?) {}

    // ==================================== Platform session ====================================

    /**
     * See documentation to [FirAbstractSessionFactory.createSourceSession]
     */
    fun createSourceSession(
        moduleData: FirModuleData,
        projectEnvironment: AbstractProjectEnvironment,
        incrementalCompilationContext: IncrementalCompilationContext?,
        extensionRegistrars: List<FirExtensionRegistrar>,
        configuration: CompilerConfiguration,
        isForLeafHmppModule: Boolean,
        init: FirSessionConfigurator.() -> Unit = {}
    ): FirSession {
        return createSourceSession(
            moduleData,
            context = null,
            extensionRegistrars,
            configuration,
            isForLeafHmppModule,
            init,
            createProviders = { session, kotlinScopeProvider, symbolProvider, generatedSymbolsProvider ->
                var symbolProviderForBinariesFromIncrementalCompilation: MetadataSymbolProvider? = null
                incrementalCompilationContext?.let {
                    val precompiledBinariesPackagePartProvider = it.precompiledBinariesPackagePartProvider
                    if (precompiledBinariesPackagePartProvider != null && it.precompiledBinariesFileScope != null) {
                        val moduleDataProvider = SingleModuleDataProvider(moduleData)
                        symbolProviderForBinariesFromIncrementalCompilation =
                            MetadataSymbolProvider(
                                session,
                                moduleDataProvider,
                                kotlinScopeProvider,
                                precompiledBinariesPackagePartProvider as PackageAndMetadataPartProvider,
                                projectEnvironment.getKotlinClassFinder(it.precompiledBinariesFileScope) as KotlinMetadataFinder,
                                defaultDeserializationOrigin = FirDeclarationOrigin.Precompiled
                            )
                    }
                }

                SourceProviders(
                    listOfNotNull(
                        symbolProvider,
                        *(incrementalCompilationContext?.previousFirSessionsSymbolProviders?.toTypedArray() ?: emptyArray()),
                        symbolProviderForBinariesFromIncrementalCompilation,
                        generatedSymbolsProvider,
                    )
                )
            }
        )
    }

    override fun createKotlinScopeProviderForSourceSession(
        moduleData: FirModuleData,
        languageVersionSettings: LanguageVersionSettings,
    ): FirKotlinScopeProvider {
        return if (languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation)) {
            /**
             * For stdlib and builtin compilation, we don't want to hide @PlatformDependent declarations from the metadata
             */
            FirKotlinScopeProvider { _, declaredScope, _, _, _ -> declaredScope }
        } else {
            FirKotlinScopeProvider()
        }
    }

    override fun FirSessionConfigurator.registerPlatformCheckers(c: Nothing?) {}

    override fun FirSessionConfigurator.registerExtraPlatformCheckers(c: Nothing?) {}

    override fun FirSession.registerSourceSessionComponents(c: Nothing?) {
        register(FirDefaultImportsProviderHolder::class, FirDefaultImportsProviderHolder(CommonDefaultImportsProvider))
    }

    override val requiresSpecialSetupOfSourceProvidersInHmppCompilation: Boolean
        get() = false

    // ==================================== Common parts ====================================

    // ==================================== Utilities ====================================

}

object FirMetadataSessionFactory : AbstractFirMetadataSessionFactory() {
    override fun createPlatformSpecificSharedProviders(
        session: FirSession,
        moduleData: FirModuleData,
        scopeProvider: FirKotlinScopeProvider,
        context: Nothing?,
    ): List<FirSymbolProvider> {
        return listOfNotNull(
            runUnless(session.languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation)) {
                FirFallbackBuiltinSymbolProvider(session, moduleData, scopeProvider)
            },
            FirCloneableSymbolProvider(session, moduleData, scopeProvider),
        )
    }

    override val createSeparateSharedProvidersInHmppCompilation: Boolean
        get() = false
}

object FirMetadataSessionFactoryForHmppCompilation : AbstractFirMetadataSessionFactory() {
    override fun createPlatformSpecificSharedProviders(
        session: FirSession,
        moduleData: FirModuleData,
        scopeProvider: FirKotlinScopeProvider,
        context: Nothing?,
    ): List<FirSymbolProvider> {
        return emptyList()
    }

    override val createSeparateSharedProvidersInHmppCompilation: Boolean
        get() = true
}
