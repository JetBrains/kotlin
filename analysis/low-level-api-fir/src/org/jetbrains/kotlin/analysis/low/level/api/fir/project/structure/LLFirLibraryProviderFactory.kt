/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.createPackagePartProviderForLibrary
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirJavaFacadeForBinaries
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.project.structure.KtBinaryModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.allDirectDependenciesOfType
import org.jetbrains.kotlin.analysis.utils.errors.checkIsInstance
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.deserialization.EmptyModuleDataProvider
import org.jetbrains.kotlin.fir.deserialization.LibraryPathFilter
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.deserialization.MultipleModuleDataProvider
import org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.OptionalAnnotationClassesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.load.java.createJavaClassFinder
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory

internal object LLFirLibraryProviderFactory {
    fun createProvidersByModuleLibraryDependencies(
        session: LLFirSession,
        module: KtModule,
        kotlinScopeProvider: FirKotlinScopeProvider,
        project: Project,
        builtinTypes: BuiltinTypes,
        createSearchScopeForModules: (List<KtBinaryModule>) -> GlobalSearchScope
    ): List<FirSymbolProvider> = buildList {
        module
            .allDirectDependenciesOfType<KtBinaryModule>()
            .groupBy { it.platform }
            .forEach { (_, binaryDependencies) ->
                val moduleDataProvider = createModuleDataProviderWithLibraryDependencies(module, binaryDependencies, session)
                val scope = createSearchScopeForModules(binaryDependencies)
                val packagePartProvider = project.createPackagePartProviderForLibrary(scope)
                add(
                    JvmClassFileBasedSymbolProvider(
                        session,
                        moduleDataProvider,
                        kotlinScopeProvider,
                        packagePartProvider,
                        VirtualFileFinderFactory.getInstance(project).create(scope),
                        LLFirJavaFacadeForBinaries(session, builtinTypes, project.createJavaClassFinder(scope), moduleDataProvider)
                    )
                )
                add(OptionalAnnotationClassesProvider(session, moduleDataProvider, kotlinScopeProvider, packagePartProvider))
            }
    }

    private fun createModuleDataProviderWithLibraryDependencies(
        sourceModule: KtModule,
        binaryDependencies: List<KtBinaryModule>,
        session: LLFirSession
    ): ModuleDataProvider {
        val moduleDatas = binaryDependencies.map { LLFirKtModuleBasedModuleData(it) }

        if (moduleDatas.isEmpty()) {
            return EmptyModuleDataProvider(sourceModule.platform, sourceModule.analyzerServices)
        }

        moduleDatas.forEach { it.bindSession(session) }

        val moduleDataWithFilters: Map<FirModuleData, LibraryPathFilter.LibraryList> =
            moduleDatas.associateWith { moduleData ->
                checkIsInstance<LLFirKtModuleBasedModuleData>(moduleData)
                val ktBinaryModule = moduleData.ktModule as KtBinaryModule
                val moduleBinaryRoots = ktBinaryModule.getBinaryRoots().mapTo(mutableSetOf()) { it.toAbsolutePath() }
                LibraryPathFilter.LibraryList(moduleBinaryRoots)
            }

        return MultipleModuleDataProvider(moduleDataWithFilters)
    }
}