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
import org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.OptionalAnnotationClassesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.load.java.createJavaClassFinder
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory

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
        val moduleDataProvider = SingleModuleDataProvider(moduleData)
        val packagePartProvider = project.createPackagePartProvider(scope)
        return LLFirModuleWithDependenciesSymbolProvider(
            session,
            providers = listOfNotNull(
                JvmClassFileBasedSymbolProvider(
                    session,
                    moduleDataProvider,
                    kotlinScopeProvider,
                    packagePartProvider,
                    VirtualFileFinderFactory.getInstance(project).create(scope),
                    LLFirJavaFacadeForBinaries(session, builtinTypes, project.createJavaClassFinder(scope), moduleDataProvider)
                ),
                OptionalAnnotationClassesProvider.createIfNeeded(session, moduleDataProvider, kotlinScopeProvider, packagePartProvider),
            ),
            LLFirDependenciesSymbolProvider(session, listOf(builtinSymbolProvider)),
        )
    }

    fun createLibraryProvidersForAllProjectLibraries(
        session: LLFirSession,
        moduleData: LLFirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
        project: Project,
        builtinTypes: BuiltinTypes,
        scope: GlobalSearchScope,
    ): List<FirSymbolProvider> {
        val moduleDataProvider = SingleModuleDataProvider(moduleData)
        val packagePartProvider = project.createPackagePartProvider(scope)
        return listOfNotNull(
            JvmClassFileBasedSymbolProvider(
                session,
                moduleDataProvider,
                kotlinScopeProvider,
                packagePartProvider,
                VirtualFileFinderFactory.getInstance(project).create(scope),
                LLFirJavaFacadeForBinaries(session, builtinTypes, project.createJavaClassFinder(scope), moduleDataProvider)
            ),
            OptionalAnnotationClassesProvider.createIfNeeded(session, moduleDataProvider, kotlinScopeProvider, packagePartProvider),
        )
    }
}