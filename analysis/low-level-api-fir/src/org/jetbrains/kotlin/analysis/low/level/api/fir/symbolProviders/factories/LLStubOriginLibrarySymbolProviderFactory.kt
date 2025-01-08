/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.factories

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.moduleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLBuiltinSymbolProviderMarker
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLFirJavaSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.createNativeForwardDeclarationsSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization.BuiltinsDeserializedContainerSourceProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization.DeserializedContainerSourceProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization.JvmAndBuiltinsDeserializedContainerSourceProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization.NullDeserializedContainerSourceProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLKotlinStubBasedLibrarySymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLFirKotlinSymbolNamesProvider
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.resolve.providers.FirCompositeCachedSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.library.KLIB_METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.deserialization.METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol

/**
 * [LLLibrarySymbolProviderFactory] for [KotlinDeserializedDeclarationsOrigin.STUBS][org.jetbrains.kotlin.analysis.api.platform.KotlinDeserializedDeclarationsOrigin.STUBS].
 */
internal object LLStubOriginLibrarySymbolProviderFactory : LLLibrarySymbolProviderFactory {
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
                createStubBasedLibrarySymbolProviderForClassFiles(
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
        createStubBasedLibrarySymbolProviderForCommonMetadataFiles(
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
            createStubBasedLibrarySymbolProviderForKotlinNativeMetadataFiles(
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
            createStubBasedLibrarySymbolProviderForKotlinNativeMetadataFiles(
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
            createStubBasedLibrarySymbolProviderForKotlinNativeMetadataFiles(
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

private fun createStubBasedLibrarySymbolProviderForClassFiles(
    session: LLFirSession,
    baseScope: GlobalSearchScope,
    isFallbackDependenciesProvider: Boolean,
): FirSymbolProvider = createStubBasedLibrarySymbolProviderForScopeLimitedByFiles(
    session,
    baseScope,
    JvmAndBuiltinsDeserializedContainerSourceProvider,
    isFallbackDependenciesProvider,
) { file ->
    val extension = file.extension
    extension == JavaClassFileType.INSTANCE.defaultExtension || extension == BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION
}

private fun createStubBasedLibrarySymbolProviderForCommonMetadataFiles(
    session: LLFirSession,
    baseScope: GlobalSearchScope,
    isFallbackDependenciesProvider: Boolean,
): FirSymbolProvider = createStubBasedLibrarySymbolProviderForScopeLimitedByFiles(
    session,
    baseScope,
    NullDeserializedContainerSourceProvider,
    isFallbackDependenciesProvider,
) { file ->
    val extension = file.extension
    extension == BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION ||
            extension == METADATA_FILE_EXTENSION ||
            // klib metadata symbol provider
            extension == KLIB_METADATA_FILE_EXTENSION
}

private fun createStubBasedLibrarySymbolProviderForKotlinNativeMetadataFiles(
    session: LLFirSession,
    baseScope: GlobalSearchScope,
    isFallbackDependenciesProvider: Boolean,
): FirSymbolProvider = createStubBasedLibrarySymbolProviderForScopeLimitedByFiles(
    session,
    baseScope,
    NullDeserializedContainerSourceProvider,
    isFallbackDependenciesProvider,
) { file -> file.extension == KLIB_METADATA_FILE_EXTENSION }

private fun createStubBasedLibrarySymbolProviderForScopeLimitedByFiles(
    session: LLFirSession,
    baseScope: GlobalSearchScope,
    deserializedContainerSourceProvider: DeserializedContainerSourceProvider,
    isFallbackDependenciesProvider: Boolean,
    fileFilter: (VirtualFile) -> Boolean,
): LLKotlinStubBasedLibrarySymbolProvider {
    return createFirSymbolProviderForScopeLimitedByFiles(
        session.project, baseScope, fileFilter,
        symbolProviderFactory = { reducedScope: GlobalSearchScope ->
            LLKotlinStubBasedLibrarySymbolProvider(
                session,
                deserializedContainerSourceProvider,
                reducedScope,
                isFallbackDependenciesProvider,
            )
        }
    )
}

private fun <T : FirSymbolProvider> createFirSymbolProviderForScopeLimitedByFiles(
    project: Project,
    baseScope: GlobalSearchScope,
    fileFilter: (VirtualFile) -> Boolean,
    symbolProviderFactory: (reducedScope: GlobalSearchScope) -> T,
): T {
    val scopeWithFileFiltering = object : DelegatingGlobalSearchScope(project, baseScope) {
        override fun contains(file: VirtualFile): Boolean =
            fileFilter(file) && super.contains(file)
    }

    return symbolProviderFactory(scopeWithFileFiltering)
}

private class StubBasedBuiltInsSymbolProvider(session: LLFirSession) : LLKotlinStubBasedLibrarySymbolProvider(
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
