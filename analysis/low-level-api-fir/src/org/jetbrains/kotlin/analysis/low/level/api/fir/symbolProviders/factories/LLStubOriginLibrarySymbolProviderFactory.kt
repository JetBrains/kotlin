/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.factories

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.moduleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization.BuiltinsDeserializedContainerSourceProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization.JvmAndBuiltinsDeserializedContainerSourceProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization.NullDeserializedContainerSourceProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLKotlinStubBasedLibrarySymbolProvider
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.java.deserialization.FirJvmDeserializationExtension
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
internal object LLStubOriginLibrarySymbolProviderFactory : LLLibrarySymbolProviderFactory {
    override fun createJvmLibrarySymbolProvider(
        session: LLFirSession,
        firJavaFacade: FirJavaFacade,
        packagePartProvider: PackagePartProvider,
        scope: GlobalSearchScope,
    ): List<FirSymbolProvider> {
        return buildList {
            //stub based provider here works over kotlin-only indices and thus provides only kotlin declarations
            //in order to find java declarations, one need to explicitly setup java symbol provider.
            //for ProtoBuf based provider (used in compiler), there is no need in separated java provider,
            //because all declarations are retrieved at once and are not distinguished
            add(
                JvmLibraryStubBasedSymbolProvider(
                    session,
                    scope,
                )
            )
            add(LLFirJavaSymbolProvider(session, firJavaFacade, scope))
        }
    }

    override fun createMetadataLibrarySymbolProvider(
        session: LLFirSession,
        packagePartProvider: PackagePartProvider,
        scope: GlobalSearchScope,
    ): List<FirSymbolProvider> = listOf(
        LLKotlinStubBasedLibrarySymbolProvider(session, NullDeserializedContainerSourceProvider, scope),
    )

    override fun createNativeLibrarySymbolProvider(
        session: LLFirSession,
        scope: GlobalSearchScope,
    ): List<FirSymbolProvider> = listOfNotNull(
        createStubBasedLibrarySymbolProviderForKlib(session, scope),
        createNativeForwardDeclarationsSymbolProvider(session),
    )

    override fun createJsLibrarySymbolProvider(
        session: LLFirSession,
        scope: GlobalSearchScope,
    ): List<FirSymbolProvider> = listOf(
        createStubBasedLibrarySymbolProviderForKlib(session, scope),
    )

    override fun createWasmLibrarySymbolProvider(
        session: LLFirSession,
        scope: GlobalSearchScope,
    ): List<FirSymbolProvider> = listOf(
        createStubBasedLibrarySymbolProviderForKlib(session, scope),
    )

    override fun createBuiltinsSymbolProvider(session: LLFirSession): List<FirSymbolProvider> {
        return listOf(StubBasedBuiltInsSymbolProvider(session))
    }
}

private fun createStubBasedLibrarySymbolProviderForKlib(
    session: LLFirSession,
    baseScope: GlobalSearchScope,
): FirSymbolProvider = LLKotlinStubBasedLibrarySymbolProvider(
    session,
    NullDeserializedContainerSourceProvider,
    baseScope,
)

/**
 * A symbol provider for JVM libraries which hides the builtin classes whose deserialized supertypes depend on the SDK
 * (see [FirJvmDeserializationExtension.CLASSES_WITH_SDK_DEPENDENT_SUPERTYPES]) from dependent sessions.
 *
 * A library session is shared between all use-site modules which depend on the library, so the use-site modules must
 * not resolve such classes from it: e.g., `kotlin.Int` should receive the `java.lang.constant.Constable` supertype
 * only when the use-site module's JDK declares this interface (KT-29858). Instead, use-site modules resolve these
 * classes through the fallback builtins session, which is keyed by the use-site module's SDK
 * (see [LLFirBuiltinsSessionFactory.getBuiltinsSession][org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.factory.LLFirBuiltinsSessionFactory.getBuiltinsSession]).
 *
 * Module-internal accesses (e.g., PSI-anchored resolution of the library's own decompiled declarations) still work with
 * the library's own copies via [LLPartiallyHidingSymbolProvider].
 */
private class JvmLibraryStubBasedSymbolProvider(
    session: LLFirSession,
    scope: GlobalSearchScope,
) : LLKotlinStubBasedLibrarySymbolProvider(
    session,
    JvmAndBuiltinsDeserializedContainerSourceProvider,
    scope,
), LLPartiallyHidingSymbolProvider {
    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        if (classId.outermostClassId in FirJvmDeserializationExtension.CLASSES_WITH_SDK_DEPENDENT_SUPERTYPES) return null
        return super.getClassLikeSymbolByClassId(classId)
    }

    override fun getClassLikeSymbolByClassIdIncludingHidden(classId: ClassId): FirClassLikeSymbol<*>? =
        super.getClassLikeSymbolByClassId(classId)
}

private class StubBasedBuiltInsSymbolProvider(session: LLFirSession) : LLKotlinStubBasedLibrarySymbolProvider(
    session,
    BuiltinsDeserializedContainerSourceProvider,
    BuiltinsVirtualFileProvider.getInstance().createBuiltinsScope(session.project),
) {
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
