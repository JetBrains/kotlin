/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltInsVirtualFileProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.LLFirLibrarySymbolProviderFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.LLFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirJavaSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLFirKotlinSymbolNamesProvider
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.resolve.providers.FirCompositeCachedSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtFile

@LLFirInternals
class LLStubBasedLibrarySymbolProviderFactory(private val project: Project) : LLFirLibrarySymbolProviderFactory() {
    override fun createJvmLibrarySymbolProvider(
        session: FirSession,
        moduleData: LLFirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
        moduleDataProvider: SingleModuleDataProvider,
        firJavaFacade: FirJavaFacade,
        packagePartProvider: PackagePartProvider,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider> {
        return buildList {
            //stub based provider here works over kotlin-only indices and thus provides only kotlin declarations
            //in order to find java declarations, one need to explicitly setup java symbol provider.
            //for ProtoBuf based provider (used in compiler), there is no need in separated java provider,
            //because all declarations are retrieved at once and are not distinguished
            add(
                createStubBasedFirSymbolProviderForClassFiles(
                    project,
                    scope,
                    session,
                    moduleDataProvider,
                    kotlinScopeProvider,
                    isFallbackDependenciesProvider,
                )
            )
            add(LLFirJavaSymbolProvider(session, moduleData, project, scope))
        }
    }

    override fun createCommonLibrarySymbolProvider(
        session: FirSession,
        moduleData: LLFirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
        moduleDataProvider: SingleModuleDataProvider,
        packagePartProvider: PackagePartProvider,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider> = listOf(
        createStubBasedFirSymbolProviderForCommonMetadataFiles(
            project = project,
            baseScope = scope,
            session = session,
            moduleDataProvider = moduleDataProvider,
            kotlinScopeProvider = kotlinScopeProvider,
            isFallbackDependenciesProvider = isFallbackDependenciesProvider,
        )
    )

    override fun createNativeLibrarySymbolProvider(
        session: FirSession,
        moduleData: LLFirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
        moduleDataProvider: SingleModuleDataProvider,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider> {
        return listOf(
            createStubBasedFirSymbolProviderForKotlinNativeMetadataFiles(
                project,
                scope,
                session,
                moduleDataProvider,
                kotlinScopeProvider,
                isFallbackDependenciesProvider,
            )
        )
    }

    override fun createJsLibrarySymbolProvider(
        session: FirSession,
        moduleData: LLFirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
        moduleDataProvider: SingleModuleDataProvider,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider> {
        return listOf(
            createStubBasedFirSymbolProviderForKotlinNativeMetadataFiles(
                project,
                scope,
                session,
                moduleDataProvider,
                kotlinScopeProvider,
                isFallbackDependenciesProvider,
            ),
        )
    }

    override fun createBuiltinsSymbolProvider(
        session: FirSession,
        moduleData: LLFirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider
    ): List<FirSymbolProvider> {
        return listOf(
            StubBasedBuiltInsSymbolProvider(project, session, moduleData, kotlinScopeProvider)
        )
    }
}

private class StubBasedBuiltInsSymbolProvider(
    project: Project,
    session: FirSession,
    moduleData: LLFirModuleData,
    kotlinScopeProvider: FirKotlinScopeProvider,
) : StubBasedFirDeserializedSymbolProvider(
    session,
    SingleModuleDataProvider(moduleData),
    kotlinScopeProvider,
    project,
    createBuiltInsScope(project),
    isFallbackDependenciesProvider = false,
) {
    private val syntheticFunctionInterfaceProvider = FirBuiltinSyntheticFunctionInterfaceProvider(
        session,
        moduleData,
        kotlinScopeProvider
    )

    override val symbolNamesProvider: FirSymbolNamesProvider = FirCompositeCachedSymbolNamesProvider(
        session,
        listOf(
            LLFirKotlinSymbolNamesProvider(declarationProvider, allowKotlinPackage),
            syntheticFunctionInterfaceProvider.symbolNamesProvider,
        ),
    )

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        return super.getClassLikeSymbolByClassId(classId)
            ?: syntheticFunctionInterfaceProvider.getClassLikeSymbolByClassId(classId)
    }

    override fun getDeclarationOriginFor(file: KtFile): FirDeclarationOrigin {
        // this provider operates only on builtins files, no need to check anything
        return FirDeclarationOrigin.BuiltIns
    }
}

private fun createBuiltInsScope(project: Project): GlobalSearchScope {
    val builtInFiles = BuiltInsVirtualFileProvider.getInstance().getBuiltInVirtualFiles()
    return GlobalSearchScope.filesScope(project, builtInFiles)
}