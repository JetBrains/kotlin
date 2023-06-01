/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirDependenciesSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirModuleWithDependenciesSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirJavaFacadeForBinaries
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.providers.createPackagePartProvider
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.java.deserialization.OptionalAnnotationClassesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.load.java.createJavaClassFinder
import org.jetbrains.kotlin.utils.addIfNotNull

internal object LLFirLibraryProviderFactory {
    fun createLibraryProvidersForScope(
        session: LLFirSession,
        moduleData: LLFirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
        project: Project,
        builtinTypes: BuiltinTypes,
        scope: GlobalSearchScope,
        builtinSymbolProvider: FirSymbolProvider,
    ): LLFirModuleWithDependenciesSymbolProvider {
        return LLFirModuleWithDependenciesSymbolProvider(
            session,
            providers = createProjectLibraryProvidersForScope(
                session,
                moduleData,
                kotlinScopeProvider,
                project,
                builtinTypes,
                scope
            ),
            LLFirDependenciesSymbolProvider(session, listOf(builtinSymbolProvider)),
        )
    }

    fun createProjectLibraryProvidersForScope(
        session: LLFirSession,
        moduleData: LLFirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
        project: Project,
        builtinTypes: BuiltinTypes,
        scope: GlobalSearchScope,
    ): List<FirSymbolProvider> {
        val moduleDataProvider = SingleModuleDataProvider(moduleData)
        val packagePartProvider = project.createPackagePartProvider(scope)
        return buildList {
            val firJavaFacade = LLFirJavaFacadeForBinaries(session, builtinTypes, project.createJavaClassFinder(scope), moduleDataProvider)
            val service = project.getService(JvmFirDeserializedSymbolProviderFactory::class.java)
            addAll(
                service.createJvmFirDeserializedSymbolProviders(
                    project,
                    session,
                    moduleData,
                    kotlinScopeProvider,
                    moduleDataProvider,
                    firJavaFacade,
                    packagePartProvider,
                    scope
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
}