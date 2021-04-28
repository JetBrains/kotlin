/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckers
import org.jetbrains.kotlin.fir.analysis.checkersComponent
import org.jetbrains.kotlin.fir.analysis.extensions.additionalCheckers
import org.jetbrains.kotlin.fir.checkers.registerCommonCheckers
import org.jetbrains.kotlin.fir.checkers.registerJvmCheckers
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

@OptIn(PrivateSessionConstructor::class, SessionConfiguration::class)
object FirSessionFactory {
    class FirSessionConfigurator(private val session: FirSession) {
        private val registeredExtensions = mutableListOf<BunchOfRegisteredExtensions>(BunchOfRegisteredExtensions.empty())

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

    fun createJavaModuleBasedSession(
        moduleInfo: ModuleInfo,
        sessionProvider: FirProjectSessionProvider,
        scope: GlobalSearchScope,
        project: Project,
        additionalPackagePartProvider: PackagePartProvider? = null,
        additionalScope: GlobalSearchScope? = null,
        dependenciesProvider: FirSymbolProvider? = null,
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
        lookupTracker: LookupTracker? = null,
        init: FirSessionConfigurator.() -> Unit = {}
    ): FirJavaModuleBasedSession {
        return FirJavaModuleBasedSession(moduleInfo, sessionProvider).apply {
            registerCliCompilerOnlyComponents()
            registerCommonComponents(languageVersionSettings)
            registerResolveComponents(lookupTracker)
            registerJavaSpecificResolveComponents()

            val kotlinScopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)

            val firProvider = FirProviderImpl(this, kotlinScopeProvider)
            register(FirProvider::class, firProvider)

            val deserializedJvmSymbolsProvider =
                if (additionalPackagePartProvider == null) null
                else {
                    val javaSymbolProvider = JavaSymbolProvider(this, project, additionalScope ?: scope)

                    makeDeserializedJvmSymbolsProvider(
                        project, additionalScope ?: scope, additionalPackagePartProvider, javaSymbolProvider, kotlinScopeProvider
                    )
                }

            register(
                FirSymbolProvider::class,
                FirCompositeSymbolProvider(
                    this,
                    listOfNotNull(
                        firProvider.symbolProvider,
                        JavaSymbolProvider(this, project, scope),
                        dependenciesProvider ?: FirDependenciesSymbolProviderImpl(this),
                        deserializedJvmSymbolsProvider
                    )
                ) as FirSymbolProvider
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
        moduleInfo: ModuleInfo,
        sessionProvider: FirProjectSessionProvider,
        scope: GlobalSearchScope,
        project: Project,
        packagePartProvider: PackagePartProvider,
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
    ): FirLibrarySession {
        return FirLibrarySession(moduleInfo, sessionProvider).apply {
            registerCliCompilerOnlyComponents()
            registerCommonComponents(languageVersionSettings)

            val javaSymbolProvider = JavaSymbolProvider(this, project, scope)

            val kotlinScopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)

            val deserializedJvmSymbolsProvider = makeDeserializedJvmSymbolsProvider(
                project, scope, packagePartProvider, javaSymbolProvider, kotlinScopeProvider
            )

            val symbolProvider = FirCompositeSymbolProvider(
                this,
                listOf(
                    deserializedJvmSymbolsProvider,
                    FirBuiltinSymbolProvider(this, kotlinScopeProvider),
                    FirCloneableSymbolProvider(this, kotlinScopeProvider),
                    javaSymbolProvider,
                    FirDependenciesSymbolProviderImpl(this)
                )
            )
            register(FirSymbolProvider::class, symbolProvider)
            register(FirProvider::class, FirLibrarySessionProvider(symbolProvider))
        }
    }

    private fun FirSession.makeDeserializedJvmSymbolsProvider(
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
            this, kotlinScopeProvider, packagePartProvider, kotlinClassFinder, javaSymbolProvider, javaClassFinder
        )
    }

    @TestOnly
    fun createEmptySession(): FirSession {
        return object : FirSession(null) {}
    }
}
