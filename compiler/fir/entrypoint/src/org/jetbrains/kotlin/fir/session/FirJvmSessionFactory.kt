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
import org.jetbrains.kotlin.fir.java.FirJavaFacade
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
import org.jetbrains.kotlin.jvm.environment.JvmCompilationEnvironment
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
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
                val classpath = moduleDataProvider.getModuleDataPaths(moduleData)
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { JvmClasspath.Roots(it.toList()) }
                    ?: context.librariesClasspath
                val kotlinClassFinder = projectEnvironment.getKotlinClassFinder(classpath)
                val javaFacade = context.javaInterop.createBinaryJavaFacade(session, moduleData, context.librariesClasspath)
                listOfNotNull(
                    JvmClassFileBasedSymbolProvider(
                        session,
                        moduleDataProvider,
                        kotlinScopeProvider,
                        context.packagePartProviderForLibraries,
                        kotlinClassFinder,
                        javaFacade,
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
            inlineConstTracker = c.inlineConstTracker
        )
    }

    // ==================================== Platform session ====================================

    /**
     * See documentation to [FirAbstractSessionFactory.createSourceSession]
     */
    fun createSourceSession(
        moduleData: FirModuleData,
        createIncrementalCompilationSymbolProviders: (FirSession) -> FirJvmIncrementalCompilationSymbolProviders?,
        extensionRegistrars: List<FirExtensionRegistrar>,
        configuration: CompilerConfiguration,
        context: Context,
        kmpModuleKind: KmpModuleKind,
        init: FirSessionConfigurator.() -> Unit,
    ): FirSession {
        val projectEnvironment = context.projectEnvironment
        return createSourceSession(
            moduleData,
            context = context,
            extensionRegistrars,
            configuration,
            kmpModuleKind,
            init,
            createProviders = { session, kotlinScopeProvider, symbolProvider, generatedSymbolsProvider ->
                val javaFacade = context.javaInterop.createJavaSourcesFacade(session, moduleData)
                val javaSymbolProvider =
                    JavaSymbolProvider(session, javaFacade)
                session.register(JavaSymbolProvider::class, javaSymbolProvider)

                val incrementalCompilationSymbolProviders = createIncrementalCompilationSymbolProviders(session)

                val providers = listOfNotNull(
                    symbolProvider,
                    generatedSymbolsProvider,
                    javaSymbolProvider,
                    initializeForStdlibIfNeeded(projectEnvironment, session, kotlinScopeProvider),
                )
                SourceProviders(
                    providers,
                    incrementalProvider = incrementalCompilationSymbolProviders?.symbolProviderForBinariesFromIncrementalCompilation,
                    incrementalCompilationSymbolProviders?.optionalAnnotationClassesProviderForBinariesFromIncrementalCompilation
                )
            }
        ).also {
            context.javaInterop.registerKotlinDeclarationsForJava(it)
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
        val projectEnvironment: JvmCompilationEnvironment,
        val librariesClasspath: JvmClasspath,
        val registerJvmDeserializationExtension: Boolean,
        val inlineConstTracker: InlineConstTracker?,
        /**
         * The Java implementation of this compilation: every session created with this context, and every
         * other consumer which needs Java declarations of some scope (the symbol provider for the precompiled
         * binaries of incremental compilation, the JVM interpretation of an HMPP common fragment's
         * classpath), builds its [FirJavaFacade] here instead of choosing an implementation on its own, and
         * a source session exposes its Kotlin declarations to Java resolution through the same object.
         *
         * There is no default: `VfsBasedProjectEnvironment.psiJavaInterop()` (the PSI view) and
         * `createJavaDirectJavaInterop` (java-direct) are peers, and a consumer which does not state
         * its choice is a consumer which has not made one.
         */
        val javaInterop: FirJavaInterop,
    ) {
        constructor(
            configuration: CompilerConfiguration,
            projectEnvironment: JvmCompilationEnvironment,
            librariesClasspath: JvmClasspath,
            javaInterop: FirJavaInterop,
            registerJvmDeserializationExtension: Boolean = true,
        ) : this(
            jvmTarget = configuration.jvmTarget ?: JvmTarget.DEFAULT,
            projectEnvironment,
            librariesClasspath,
            registerJvmDeserializationExtension = registerJvmDeserializationExtension,
            inlineConstTracker = configuration.inlineConstTracker,
            javaInterop = javaInterop,
        )

        val packagePartProviderForLibraries: PackagePartProvider = projectEnvironment.getPackagePartProvider(librariesClasspath)

        val predefinedJavaComponents: FirSharableJavaComponents = FirSharableJavaComponents(firCachesFactoryForCliMode)
    }

    private fun initializeForStdlibIfNeeded(
        projectEnvironment: JvmCompilationEnvironment,
        session: FirSession,
        kotlinScopeProvider: FirKotlinScopeProvider,
    ): FirSymbolProvider? {
        return runIf(
            session.languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation) &&
                    !session.moduleData.isCommon
                    && session.moduleData.dependsOnDependencies.isEmpty()
        ) {
            val kotlinClassFinder = projectEnvironment.getKotlinClassFinder(JvmClasspath.ProjectLibraries())
            FirJvmClasspathBuiltinSymbolProvider(
                session,
                session.moduleData,
                kotlinScopeProvider
            ) { kotlinClassFinder.findBuiltInsData(it) }
        }
    }

    fun initializeBuiltinsProvider(
        session: FirSession,
        builtinsModuleData: FirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
        kotlinClassFinder: KotlinClassFinder,
    ): FirJvmBuiltinsSymbolProvider = FirJvmBuiltinsSymbolProvider(
        session,
        FirFallbackBuiltinSymbolProvider(session, builtinsModuleData, kotlinScopeProvider)
    ) { kotlinClassFinder.findBuiltInsData(it) }
}
