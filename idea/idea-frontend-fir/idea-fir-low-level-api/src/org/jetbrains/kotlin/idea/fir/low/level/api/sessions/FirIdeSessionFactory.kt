/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.sessions

import com.intellij.openapi.module.impl.scopes.ModuleWithDependentsScope
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.checkers.registerCommonCheckers
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.KotlinDeserializedJvmSymbolsProvider
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCloneableSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.resolve.transformers.PhasedFirFileResolver
import org.jetbrains.kotlin.fir.scopes.KotlinScopeProvider
import org.jetbrains.kotlin.fir.session.FirSessionFactory
import org.jetbrains.kotlin.fir.session.registerCommonComponents
import org.jetbrains.kotlin.fir.session.registerJavaSpecificComponents
import org.jetbrains.kotlin.fir.session.registerResolveComponents
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.resolve.IDEPackagePartProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.FirPhaseRunner
import org.jetbrains.kotlin.idea.fir.low.level.api.IdePhasedFirFileResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.IdeSessionComponents
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCacheImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.FirIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.util.collectTransitiveDependenciesWithSelf
import org.jetbrains.kotlin.load.java.JavaClassFinderImpl
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory

@OptIn(PrivateSessionConstructor::class, SessionConfiguration::class)
internal object FirIdeSessionFactory {
    /**
     * Should be invoked only under a [moduleInfo]-based lock
     */
    fun createSourcesSession(
        project: Project,
        moduleInfo: ModuleSourceInfo,
        firPhaseRunner: FirPhaseRunner,
        sessionProvider: FirIdeSessionProvider,
        librariesSession: FirIdeLibrariesSession,
        init: FirSessionFactory.FirSessionConfigurator.() -> Unit = {}
    ): FirIdeSourcesSession {
        val scopeProvider = KotlinScopeProvider(::wrapScopeWithJvmMapped)
        val firBuilder = FirFileBuilder(scopeProvider, firPhaseRunner)
        val dependentModules = moduleInfo.collectTransitiveDependenciesWithSelf().filterIsInstance<ModuleSourceInfo>()
        val searchScope = ModuleWithDependentsScope(project, dependentModules.map { it.module })
        return FirIdeSourcesSession(moduleInfo, sessionProvider, searchScope, firBuilder).apply {
            val cache = ModuleFileCacheImpl(this)
            val phasedFirFileResolver = IdePhasedFirFileResolver(FirLazyDeclarationResolver(firFileBuilder), cache)

            registerCommonComponents()
            registerResolveComponents()

            val provider = FirIdeProvider(
                project,
                this,
                moduleInfo,
                scopeProvider,
                firFileBuilder,
                cache,
                searchScope
            )

            register(FirProvider::class, provider)
            register(FirIdeProvider::class, provider)

            register(PhasedFirFileResolver::class, phasedFirFileResolver)

            register(
                FirSymbolProvider::class,
                FirCompositeSymbolProvider(
                    this,
                    @OptIn(ExperimentalStdlibApi::class)
                    buildList {
                        add(provider.symbolProvider)
                        add(JavaSymbolProvider(this@apply, sessionProvider.project, searchScope))
                        add(librariesSession.firSymbolProvider)
                    }
                ) as FirSymbolProvider
            )
            registerJavaSpecificComponents()
            FirSessionFactory.FirSessionConfigurator(this).apply {
                registerCommonCheckers()
                init()
            }.configure()
        }
    }

    /**
     * Should be invoked only under a [moduleInfo]-based lock
     */
    fun createLibrarySession(
        moduleInfo: ModuleSourceInfo,
        sessionProvider: FirIdeSessionProvider,
        project: Project,
    ): FirIdeLibrariesSession {
        val searchScope = moduleInfo.module.moduleWithLibrariesScope
        val javaClassFinder = JavaClassFinderImpl().apply {
            setProjectInstance(project)
            setScope(searchScope)
        }
        val packagePartProvider = IDEPackagePartProvider(searchScope)

        val kotlinClassFinder = VirtualFileFinderFactory.getInstance(project).create(searchScope)
        return FirIdeLibrariesSession(moduleInfo, sessionProvider, searchScope).apply {
            registerCommonComponents()

            val javaSymbolProvider = JavaSymbolProvider(this, sessionProvider.project, searchScope)

            val kotlinScopeProvider = KotlinScopeProvider(::wrapScopeWithJvmMapped)
            register(IdeSessionComponents::class, IdeSessionComponents.create(this))
            register(
                FirSymbolProvider::class,
                FirCompositeSymbolProvider(
                    this,
                    listOf(
                        KotlinDeserializedJvmSymbolsProvider(
                            this,
                            project,
                            packagePartProvider,
                            javaSymbolProvider,
                            kotlinClassFinder,
                            javaClassFinder,
                            kotlinScopeProvider
                        ),
                        FirBuiltinSymbolProvider(this, kotlinScopeProvider),
                        FirCloneableSymbolProvider(this, kotlinScopeProvider),
                        javaSymbolProvider,
                    )
                )
            )
        }
    }
}
