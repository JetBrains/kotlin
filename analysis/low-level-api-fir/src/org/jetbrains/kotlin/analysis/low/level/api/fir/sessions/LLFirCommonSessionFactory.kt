/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.LLFirLibrarySymbolProviderFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.LLFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirModuleWithDependenciesSymbolProvider
import org.jetbrains.kotlin.analysis.project.structure.KtBinaryModule
import org.jetbrains.kotlin.analysis.project.structure.KtDanglingFileModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.providers.createPackagePartProvider
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmTypeMapper
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.java.deserialization.OptionalAnnotationClassesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.FirSessionFactoryHelper.registerDefaultComponents
import org.jetbrains.kotlin.platform.has
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.utils.addIfNotNull

@OptIn(SessionConfiguration::class)
internal class LLFirCommonSessionFactory(project: Project) : LLFirAbstractSessionFactory(project) {
    override fun createSourcesSession(module: KtSourceModule): LLFirSourcesSession {
        return doCreateSourcesSession(module) { context ->
            register(
                FirSymbolProvider::class,
                LLFirModuleWithDependenciesSymbolProvider(
                    this,
                    providers = listOf(
                        context.firProvider.symbolProvider,
                    ),
                    context.dependencyProvider,
                )
            )

            registerPlatformSpecificComponentsIfAny(module)
        }
    }

    override fun createLibrarySession(module: KtModule): LLFirLibraryOrLibrarySourceResolvableModuleSession {
        return doCreateLibrarySession(module) { context ->
            register(
                FirSymbolProvider::class,
                LLFirModuleWithDependenciesSymbolProvider(
                    this,
                    providers = listOf(
                        context.firProvider.symbolProvider,
                    ),
                    context.dependencyProvider,
                )
            )

            registerPlatformSpecificComponentsIfAny(module)
        }
    }

    override fun createBinaryLibrarySession(module: KtBinaryModule): LLFirLibrarySession {
        return doCreateBinaryLibrarySession(module) {
            registerPlatformSpecificComponentsIfAny(module)
        }
    }

    override fun createDanglingFileSession(module: KtDanglingFileModule, contextSession: LLFirSession): LLFirSession {
        return doCreateDanglingFileSession(module, contextSession) {
            register(
                FirSymbolProvider::class,
                LLFirModuleWithDependenciesSymbolProvider(
                    this,
                    providers = listOf(
                        firProvider.symbolProvider,
                    ),
                    dependencyProvider,
                )
            )

            registerPlatformSpecificComponentsIfAny(module)
        }
    }

    override fun createProjectLibraryProvidersForScope(
        session: LLFirSession,
        moduleData: LLFirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
        project: Project,
        builtinTypes: BuiltinTypes,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider> {
        val moduleDataProvider = SingleModuleDataProvider(moduleData)
        val packagePartProvider = project.createPackagePartProvider(scope)
        return buildList {
            addAll(
                LLFirLibrarySymbolProviderFactory.getService(project).createCommonLibrarySymbolProvider(
                    session,
                    moduleData,
                    kotlinScopeProvider,
                    moduleDataProvider,
                    packagePartProvider,
                    scope,
                    isFallbackDependenciesProvider,
                )
            )

            addIfNotNull(
                OptionalAnnotationClassesProvider.createIfNeeded(
                    session,
                    moduleDataProvider,
                    kotlinScopeProvider,
                    packagePartProvider
                )
            )
        }
    }

    private fun LLFirSession.registerPlatformSpecificComponentsIfAny(module: KtModule) {
        if (module.platform.has<JvmPlatform>())
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))
    }
}
