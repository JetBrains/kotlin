/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.factories

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.moduleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLBuiltinSymbolProviderMarker
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLFirJavaSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.createNativeForwardDeclarationsSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization.BuiltinsDeserializedContainerSourceProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.StubBasedFirDeserializedSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLFirKotlinSymbolNamesProvider
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.resolve.providers.FirCompositeCachedSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtFile

/**
 * [LLLibrarySymbolProviderFactory] for [KotlinDeserializedDeclarationsOrigin.STUBS][org.jetbrains.kotlin.analysis.api.platform.KotlinDeserializedDeclarationsOrigin.STUBS].
 */
internal object LLStubBasedLibrarySymbolProviderFactory : LLLibrarySymbolProviderFactory {
    override fun createJvmLibrarySymbolProvider(
        session: LLFirSession,
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
                    session,
                    scope,
                    isFallbackDependenciesProvider,
                )
            )
            add(LLFirJavaSymbolProvider(session, scope))
        }
    }

    override fun createCommonLibrarySymbolProvider(
        session: LLFirSession,
        packagePartProvider: PackagePartProvider,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider> = listOf(
        createStubBasedFirSymbolProviderForCommonMetadataFiles(
            session = session,
            baseScope = scope,
            isFallbackDependenciesProvider = isFallbackDependenciesProvider,
        )
    )

    override fun createNativeLibrarySymbolProvider(
        session: LLFirSession,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider> {
        return listOfNotNull(
            createStubBasedFirSymbolProviderForKotlinNativeMetadataFiles(
                session,
                scope,
                isFallbackDependenciesProvider,
            ),
            createNativeForwardDeclarationsSymbolProvider(session),
        )
    }

    override fun createJsLibrarySymbolProvider(
        session: LLFirSession,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider> {
        return listOf(
            createStubBasedFirSymbolProviderForKotlinNativeMetadataFiles(
                session,
                scope,
                isFallbackDependenciesProvider,
            ),
        )
    }

    override fun createWasmLibrarySymbolProvider(
        session: LLFirSession,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider> {
        return listOf(
            createStubBasedFirSymbolProviderForKotlinNativeMetadataFiles(
                session,
                scope,
                isFallbackDependenciesProvider,
            ),
        )
    }

    override fun createBuiltinsSymbolProvider(session: LLFirSession): List<FirSymbolProvider> {
        return listOf(StubBasedBuiltInsSymbolProvider(session))
    }
}

private class StubBasedBuiltInsSymbolProvider(session: LLFirSession) : StubBasedFirDeserializedSymbolProvider(
    session,
    BuiltinsDeserializedContainerSourceProvider,
    BuiltinsVirtualFileProvider.getInstance().createBuiltinsScope(session.project),
    isFallbackDependenciesProvider = false,
), LLBuiltinSymbolProviderMarker {
    private val syntheticFunctionInterfaceProvider = FirBuiltinSyntheticFunctionInterfaceProvider(
        session,
        session.moduleData,
        session.kotlinScopeProvider
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
