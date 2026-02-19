/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

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
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.scopes.impl.FirEnumEntriesSupport
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.load.kotlin.PackageAndMetadataPartProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.JsPlatform
import org.jetbrains.kotlin.platform.NativePlatform
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.WasmPlatform
import org.jetbrains.kotlin.platform.has
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.platform.subplatformsOfType
import org.jetbrains.kotlin.platform.toTargetPlatform
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.serialization.deserialization.KotlinMetadataFinder
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.runUnless

typealias AdditionalProvidersSupplier = (FirSession, ModuleDataProvider, FirKotlinScopeProvider, List<KotlinLibrary>) -> List<FirSymbolProvider>

@OptIn(SessionConfiguration::class)
abstract class AbstractFirMetadataSessionFactory(
    val targetPlatform: TargetPlatform,
) : FirAbstractSessionFactory<AbstractFirMetadataSessionFactory.Context>() {
    class Context(
        createJvmContext: () -> FirJvmSessionFactory.Context,
        createJsContext: () -> FirJsSessionFactory.Context,
    ) {
        val jvmContext: FirJvmSessionFactory.Context by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { createJvmContext() }
        val jsContext: FirJsSessionFactory.Context by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { createJsContext() }
    }

    // ==================================== Shared library session ====================================

    /**
     * See documentation to [FirAbstractSessionFactory.createSharedLibrarySession]
     */
    fun createSharedLibrarySession(
        mainModuleName: Name,
        languageVersionSettings: LanguageVersionSettings,
        extensionRegistrars: List<FirExtensionRegistrar>,
        context: Context,
    ): FirSession {
        return createSharedLibrarySession(
            mainModuleName,
            context,
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
        context: Context,
        additionalProviders: AdditionalProvidersSupplier? = null,
    ): FirSession {
        return createLibrarySession(
            context,
            sharedLibrarySession,
            moduleDataProvider,
            languageVersionSettings,
            extensionRegistrars,
            createSeparateSharedProvidersInHmppCompilation,
            createProviders = { session, kotlinScopeProvider ->
                buildList {
                    jarMetadataProviderComponents?.let { (packageAndMetadataPartProvider, librariesScope, projectEnvironment) ->
                        this += MetadataSymbolProvider(
                            session,
                            moduleDataProvider,
                            kotlinScopeProvider,
                            packageAndMetadataPartProvider,
                            projectEnvironment.getKotlinClassFinder(librariesScope)
                        )
                    }
                    runIf(resolvedKLibs.isNotEmpty()) {
                        this += KlibBasedSymbolProvider(
                            session,
                            moduleDataProvider,
                            kotlinScopeProvider,
                            resolvedKLibs
                        )
                    }

                    additionalProviders?.invoke(session, moduleDataProvider, kotlinScopeProvider, resolvedKLibs)
                        ?.let { this += it }
                }
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

    override fun FirSession.registerLibrarySessionComponents(c: Context) {
        register(FirEnumEntriesSupport(this))
        processPlatformsWithContext(
            c,
            onJvmPlatform = { registerLibrarySessionComponents(it) },
            onJsPlatform = { registerLibrarySessionComponents(it) },
            onWasmJsPlatform = { registerLibrarySessionComponents(c = null) },
            onWasmWasiPlatform = { registerLibrarySessionComponents(c = null) },
            onNativePlatform = { registerLibrarySessionComponents(c = null) },
        )
    }

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
        context: Context,
        isForLeafHmppModule: Boolean,
        init: FirSessionConfigurator.() -> Unit = {}
    ): FirSession {
        return createSourceSession(
            moduleData,
            context,
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

    override fun FirSessionConfigurator.registerPlatformCheckers() {
        processPlatforms(
            onJvmPlatform = { registerPlatformCheckers() },
            onJsPlatform = { registerPlatformCheckers() },
            onWasmJsPlatform = { registerPlatformCheckers() },
            onWasmWasiPlatform = { registerPlatformCheckers() },
            onNativePlatform = { registerPlatformCheckers() },
        )
    }

    override fun FirSessionConfigurator.registerExtraPlatformCheckers() {
        processPlatforms(
            onJvmPlatform = { registerExtraPlatformCheckers() },
            onJsPlatform = { registerExtraPlatformCheckers() },
            onWasmJsPlatform = { registerExtraPlatformCheckers() },
            onWasmWasiPlatform = { registerExtraPlatformCheckers() },
            onNativePlatform = { registerExtraPlatformCheckers() },
        )
    }

    override fun FirSession.registerSourceSessionComponents(c: Context) {
        processPlatformsWithContext(
            c,
            onJvmPlatform = { registerSourceSessionComponents(it) },
            onJsPlatform = { registerSourceSessionComponents(it) },
            onWasmJsPlatform = { registerSourceSessionComponents(c = null) },
            onWasmWasiPlatform = { registerSourceSessionComponents(c = null) },
            onNativePlatform = { registerSourceSessionComponents(c = null) },
        )
    }

    override val requiresSpecialSetupOfSourceProvidersInHmppCompilation: Boolean
        get() = false

    override val isFactoryForMetadataCompilation: Boolean
        get() = true

    // ==================================== Common parts ====================================

    // ==================================== Utilities ====================================

    private fun processPlatformsWithContext(
        c: Context,
        onJvmPlatform: FirJvmSessionFactory.(FirJvmSessionFactory.Context) -> Unit,
        onJsPlatform: FirJsSessionFactory.(FirJsSessionFactory.Context) -> Unit,
        onWasmJsPlatform: FirWasmSessionFactory.WasmJs.() -> Unit,
        onWasmWasiPlatform: FirWasmSessionFactory.WasmWasi.() -> Unit,
        onNativePlatform: FirNativeSessionFactory.ForMetadata.() -> Unit,
    ) {
        processPlatforms(
            onJvmPlatform = { onJvmPlatform(c.jvmContext) },
            onJsPlatform = { onJsPlatform(c.jsContext) },
            onWasmJsPlatform,
            onWasmWasiPlatform,
            onNativePlatform,
        )
    }

    private fun processPlatforms(
        onJvmPlatform: FirJvmSessionFactory.() -> Unit,
        onJsPlatform: FirJsSessionFactory.() -> Unit,
        onWasmJsPlatform: FirWasmSessionFactory.WasmJs.() -> Unit,
        onWasmWasiPlatform: FirWasmSessionFactory.WasmWasi.() -> Unit,
        onNativePlatform: FirNativeSessionFactory.ForMetadata.() -> Unit,
    ) {
        val targetPlatform = targetPlatform
        if (targetPlatform.has<JvmPlatform>()) {
            with(FirJvmSessionFactory) {
                onJvmPlatform()
            }
        }
        if (targetPlatform.has<JsPlatform>()) {
            with(FirJsSessionFactory) {
                onJsPlatform()
            }
        }
        if (targetPlatform.has<WasmPlatform>()) {
            val wasmPlatforms = targetPlatform.subplatformsOfType<WasmPlatform>().map { it.toTargetPlatform() }
            if (WasmPlatforms.unspecifiedWasmPlatform in wasmPlatforms || WasmPlatforms.wasmJs in wasmPlatforms) {
                with(FirWasmSessionFactory.WasmJs) {
                    onWasmJsPlatform()
                }
            }
            if (WasmPlatforms.wasmWasi in wasmPlatforms) {
                with(FirWasmSessionFactory.WasmWasi) {
                    onWasmWasiPlatform()
                }
            }
        }
        if (targetPlatform.has<NativePlatform>()) {
            with(FirNativeSessionFactory.ForMetadata) {
                onNativePlatform()
            }
        }
    }
}

class FirMetadataSessionFactory(targetPlatform: TargetPlatform) : AbstractFirMetadataSessionFactory(targetPlatform) {
    override fun createPlatformSpecificSharedProviders(
        session: FirSession,
        moduleData: FirModuleData,
        scopeProvider: FirKotlinScopeProvider,
        context: Context,
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

class FirMetadataSessionFactoryForHmppCompilation(targetPlatform: TargetPlatform) : AbstractFirMetadataSessionFactory(targetPlatform) {
    override fun createPlatformSpecificSharedProviders(
        session: FirSession,
        moduleData: FirModuleData,
        scopeProvider: FirKotlinScopeProvider,
        context: Context,
    ): List<FirSymbolProvider> {
        return emptyList()
    }

    override val createSeparateSharedProvidersInHmppCompilation: Boolean
        get() = true
}
