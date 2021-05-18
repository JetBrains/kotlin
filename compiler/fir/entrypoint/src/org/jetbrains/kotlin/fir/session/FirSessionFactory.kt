/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckers
import org.jetbrains.kotlin.fir.analysis.checkersComponent
import org.jetbrains.kotlin.fir.analysis.extensions.additionalCheckers
import org.jetbrains.kotlin.fir.checkers.registerCommonCheckers
import org.jetbrains.kotlin.fir.checkers.registerJvmCheckers
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.BunchOfRegisteredExtensions
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.registerExtensions
import org.jetbrains.kotlin.fir.java.*
import org.jetbrains.kotlin.fir.java.deserialization.KotlinDeserializedJvmSymbolsProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.*
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.JavaClassFinderImpl
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices

@OptIn(PrivateSessionConstructor::class, SessionConfiguration::class)
object FirSessionFactory {
    class FirSessionConfigurator(private val session: FirSession) {
        private val registeredExtensions: MutableList<BunchOfRegisteredExtensions> = mutableListOf(BunchOfRegisteredExtensions.empty())

        fun registerExtensions(extensions: BunchOfRegisteredExtensions) {
            registeredExtensions += extensions
        }

        fun useCheckers(checkers: ExpressionCheckers) {
            session.checkersComponent.register(checkers)
        }

        fun useCheckers(checkers: DeclarationCheckers) {
            session.checkersComponent.register(checkers)
        }

        fun useCheckers(checkers: TypeCheckers) {
            session.checkersComponent.register(checkers)
        }

        @SessionConfiguration
        fun configure() {
            session.extensionService.registerExtensions(registeredExtensions.reduce(BunchOfRegisteredExtensions::plus))
            session.extensionService.additionalCheckers.forEach(session.checkersComponent::register)
        }
    }

    data class ProviderAndScopeForIncrementalCompilation(
        val packagePartProvider: PackagePartProvider,
        val scope: GlobalSearchScope
    )

    fun createJavaModuleBasedSession(
        moduleData: FirModuleData,
        sessionProvider: FirProjectSessionProvider,
        scope: GlobalSearchScope,
        project: Project,
        providerAndScopeForIncrementalCompilation: ProviderAndScopeForIncrementalCompilation?,
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
        lookupTracker: LookupTracker? = null,
        init: FirSessionConfigurator.() -> Unit = {}
    ): FirSession {
        return FirCliSession(sessionProvider, FirCliSession.Kind.Source).apply session@{
            moduleData.bindSession(this@session)
            sessionProvider.registerSession(moduleData, this@session)
            registerModuleData(moduleData)
            registerCliCompilerOnlyComponents()
            registerCommonComponents(languageVersionSettings)
            registerResolveComponents(lookupTracker)
            registerJavaSpecificResolveComponents()

            val kotlinScopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)

            val firProvider = FirProviderImpl(this, kotlinScopeProvider)
            register(FirProvider::class, firProvider)

            val symbolProviderForBinariesFromIncrementalCompilation = providerAndScopeForIncrementalCompilation?.let {
                val javaSymbolProvider = JavaSymbolProvider(this, moduleData, project, it.scope)

                makeDeserializedJvmSymbolsProvider(
                    this@session,
                    SingleModuleDataProvider(moduleData),
                    project,
                    it.scope,
                    it.packagePartProvider,
                    javaSymbolProvider,
                    kotlinScopeProvider
                )
            }

            register(
                FirSymbolProvider::class,
                FirCompositeSymbolProvider(
                    this,
                    listOfNotNull(
                        firProvider.symbolProvider,
                        JavaSymbolProvider(this, moduleData, project, scope),
                        FirDependenciesSymbolProviderImpl(this),
                        symbolProviderForBinariesFromIncrementalCompilation,
                    )
                )
            )

            FirSessionConfigurator(this).apply {
                registerCommonCheckers()
                registerJvmCheckers()
                init()
            }.configure()

            PsiElementFinder.EP.getPoint(project).registerExtension(FirJavaElementFinder(this, project), project)
        }
    }

    fun createLibrarySession(
        mainModuleName: Name,
        sessionProvider: FirProjectSessionProvider,
        moduleDataProvider: ModuleDataProvider,
        scope: GlobalSearchScope,
        project: Project,
        packagePartProvider: PackagePartProvider,
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
    ): FirSession {
        return FirCliSession(sessionProvider, FirCliSession.Kind.Library).apply session@{
            moduleDataProvider.allModuleData.forEach {
                sessionProvider.registerSession(it, this)
                it.bindSession(this)
            }

            registerCliCompilerOnlyComponents()
            registerCommonComponents(languageVersionSettings)

            val javaSymbolProvider = JavaSymbolProvider(this, moduleDataProvider.allModuleData.last(), project, scope)

            val kotlinScopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)

            val deserializedProviderForIncrementalCompilation = makeDeserializedJvmSymbolsProvider(
                librarySession = this,
                moduleDataProvider,
                project,
                scope,
                packagePartProvider,
                javaSymbolProvider,
                kotlinScopeProvider
            )

            val builtinsModuleData = createModuleDataForBuiltins(
                mainModuleName,
                moduleDataProvider.platform,
                moduleDataProvider.analyzerServices
            ).also { it.bindSession(this@session) }

            val symbolProvider = FirCompositeSymbolProvider(
                this,
                listOf(
                    deserializedProviderForIncrementalCompilation,
                    FirBuiltinSymbolProvider(this, builtinsModuleData, kotlinScopeProvider),
                    FirCloneableSymbolProvider(this, builtinsModuleData, kotlinScopeProvider),
                    javaSymbolProvider, // TODO: looks like it can be removed
                    FirDependenciesSymbolProviderImpl(this)
                )
            )
            register(FirSymbolProvider::class, symbolProvider)
            register(FirProvider::class, FirLibrarySessionProvider(symbolProvider))
        }
    }

    private fun makeDeserializedJvmSymbolsProvider(
        librarySession: FirSession,
        moduleDataProvider: ModuleDataProvider,
        project: Project,
        scope: GlobalSearchScope,
        packagePartProvider: PackagePartProvider,
        javaSymbolProvider: JavaSymbolProvider,
        kotlinScopeProvider: FirKotlinScopeProvider
    ): KotlinDeserializedJvmSymbolsProvider {

        val kotlinClassFinder = VirtualFileFinderFactory.getInstance(project).create(scope)
        val javaClassFinder = JavaClassFinderImpl().apply {
            this.setProjectInstance(project)
            this.setScope(scope)
        }

        return KotlinDeserializedJvmSymbolsProvider(
            librarySession,
            moduleDataProvider,
            kotlinScopeProvider,
            packagePartProvider,
            kotlinClassFinder,
            javaSymbolProvider,
            javaClassFinder
        )
    }

    @TestOnly
    fun createEmptySession(): FirSession {
        return object : FirSession(null) {}.apply {
            val moduleData = FirModuleDataImpl(
                Name.identifier("<stub module>"),
                dependencies = emptyList(),
                dependsOnDependencies = emptyList(),
                friendDependencies = emptyList(),
                platform = JvmPlatforms.unspecifiedJvmPlatform,
                analyzerServices = JvmPlatformAnalyzerServices
            )
            registerModuleData(moduleData)
            moduleData.bindSession(this)
        }
    }

    fun createModuleDataForBuiltins(
        parentModuleName: Name,
        platform: TargetPlatform,
        analyzerServices: PlatformDependentAnalyzerServices
    ): FirModuleData {
        return DependencyListForCliModule.createDependencyModuleData(
            Name.special("<builtins of ${parentModuleName.identifier}"),
            platform,
            analyzerServices,
        )
    }
}
