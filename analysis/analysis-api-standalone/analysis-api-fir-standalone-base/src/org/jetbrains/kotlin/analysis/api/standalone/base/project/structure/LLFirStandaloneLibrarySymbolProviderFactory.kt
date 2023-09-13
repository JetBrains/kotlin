/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.project.structure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.LLFirLibrarySymbolProviderFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.LLFirModuleData
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.backend.common.CommonKLibResolver
import org.jetbrains.kotlin.fir.BinaryModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.KlibBasedSymbolProvider
import org.jetbrains.kotlin.fir.session.MetadataSymbolProvider
import org.jetbrains.kotlin.fir.session.NativeForwardDeclarationsSymbolProvider
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.impl.KlibResolvedModuleDescriptorsFactoryImpl.Companion.FORWARD_DECLARATIONS_MODULE_NAME
import org.jetbrains.kotlin.load.kotlin.PackageAndMetadataPartProvider
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.util.DummyLogger

class LLFirStandaloneLibrarySymbolProviderFactory(private val project: Project) : LLFirLibrarySymbolProviderFactory() {
    override fun createJvmLibrarySymbolProvider(
        session: FirSession,
        moduleData: LLFirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
        moduleDataProvider: SingleModuleDataProvider,
        firJavaFacade: FirJavaFacade,
        packagePartProvider: PackagePartProvider,
        scope: GlobalSearchScope,
    ): List<FirSymbolProvider> {
        return listOf(
            JvmClassFileBasedSymbolProvider(
                session,
                moduleDataProvider,
                kotlinScopeProvider,
                packagePartProvider,
                VirtualFileFinderFactory.getInstance(project).create(scope),
                firJavaFacade
            )
        )
    }

    override fun createCommonLibrarySymbolProvider(
        session: FirSession,
        moduleData: LLFirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
        moduleDataProvider: SingleModuleDataProvider,
        packagePartProvider: PackagePartProvider,
        scope: GlobalSearchScope,
    ): List<FirSymbolProvider> {
        return listOf(
            MetadataSymbolProvider(
                session,
                moduleDataProvider,
                kotlinScopeProvider,
                packagePartProvider as PackageAndMetadataPartProvider,
                VirtualFileFinderFactory.getInstance(project).create(scope),
            )
        )
    }

    override fun createNativeLibrarySymbolProvider(
        session: FirSession,
        moduleData: LLFirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
        moduleDataProvider: SingleModuleDataProvider,
        scope: GlobalSearchScope,
    ): List<FirSymbolProvider> {
        val forwardDeclarationsModuleData = BinaryModuleData.createDependencyModuleData(
            FORWARD_DECLARATIONS_MODULE_NAME,
            moduleDataProvider.platform,
            moduleDataProvider.analyzerServices,
        ).apply {
            bindSession(session)
        }
        val kLibs = moduleData.getLibraryKLibs()
        return listOfNotNull(
            KlibBasedSymbolProvider(session, moduleDataProvider, kotlinScopeProvider, kLibs),
            NativeForwardDeclarationsSymbolProvider(session, forwardDeclarationsModuleData, kotlinScopeProvider, kLibs),
        )
    }

    override fun createJsLibrarySymbolProvider(
        session: FirSession,
        moduleData: LLFirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
        moduleDataProvider: SingleModuleDataProvider,
        scope: GlobalSearchScope,
    ): List<FirSymbolProvider> {
        val kLibs = moduleData.getLibraryKLibs()

        return listOf(
            KlibBasedSymbolProvider(session, moduleDataProvider, kotlinScopeProvider, kLibs)
        )
    }


    private fun LLFirModuleData.getLibraryKLibs(): List<KotlinLibrary> {
        val ktLibraryModule = ktModule as? KtLibraryModule ?: return emptyList()
        val resolveResult = CommonKLibResolver.resolve(
            ktLibraryModule.getBinaryRoots().map { it.toString() },
            DummyLogger
        )
        return resolveResult.getFullResolvedList().map { it.library }
    }
}