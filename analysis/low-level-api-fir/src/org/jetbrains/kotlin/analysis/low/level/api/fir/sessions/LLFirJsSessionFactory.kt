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
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.checkers.FirPlatformDiagnosticSuppressor
import org.jetbrains.kotlin.fir.analysis.js.checkers.FirJsPlatformDiagnosticSuppressor
import org.jetbrains.kotlin.fir.declarations.FirTypeSpecificityComparatorProvider
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.resolve.calls.ConeCallConflictResolverFactory
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.FirSessionFactoryHelper.registerDefaultComponents
import org.jetbrains.kotlin.fir.session.JsCallConflictResolverFactory
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.js.resolve.JsTypeSpecificityComparatorWithoutDelegate

@OptIn(SessionConfiguration::class)
internal class LLFirJsSessionFactory(project: Project) : LLFirAbstractSessionFactory(project) {
    override fun createSourcesSession(module: KtSourceModule): LLFirSourcesSession {
        return doCreateSourcesSession(module) { context ->
            registerDefaultComponents()
            registerModuleIndependentJsComponents()

            register(
                FirSymbolProvider::class,
                LLFirModuleWithDependenciesSymbolProvider(
                    this,
                    providers = listOfNotNull(
                        context.firProvider.symbolProvider,
                        context.switchableExtensionDeclarationsSymbolProvider,
                        context.syntheticFunctionInterfaceProvider,
                    ),
                    context.dependencyProvider,
                )
            )
        }
    }

    override fun createLibrarySession(module: KtModule): LLFirLibraryOrLibrarySourceResolvableModuleSession {
        return doCreateLibrarySession(module) { context ->
            registerDefaultComponents()
            registerModuleIndependentJsComponents()

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
        }
    }

    override fun createBinaryLibrarySession(module: KtBinaryModule): LLFirLibrarySession {
        return doCreateBinaryLibrarySession(module) {
            registerDefaultComponents()
            registerModuleIndependentJsComponents()
        }
    }

    override fun createProjectLibraryProvidersForScope(
        session: LLFirSession,
        moduleData: LLFirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
        project: Project,
        builtinTypes: BuiltinTypes,
        scope: GlobalSearchScope,
    ): List<FirSymbolProvider> {
        val moduleDataProvider = SingleModuleDataProvider(moduleData)
        return LLFirLibrarySymbolProviderFactory.getService(project)
            .createJsLibrarySymbolProvider(session, moduleData, kotlinScopeProvider, moduleDataProvider, scope)
    }

    private fun LLFirSession.registerModuleIndependentJsComponents() {
        register(ConeCallConflictResolverFactory::class, JsCallConflictResolverFactory)
        register(
            FirTypeSpecificityComparatorProvider::class,
            FirTypeSpecificityComparatorProvider(JsTypeSpecificityComparatorWithoutDelegate(typeContext))
        )
        register(FirPlatformDiagnosticSuppressor::class, FirJsPlatformDiagnosticSuppressor())
    }
}
