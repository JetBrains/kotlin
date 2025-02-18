/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.library.KLIB_METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.serialization.deserialization.METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol

fun createStubBasedFirSymbolProviderForClassFiles(
    session: LLFirSession,
    baseScope: GlobalSearchScope,
    isFallbackDependenciesProvider: Boolean,
): FirSymbolProvider = createStubBasedFirSymbolProviderForScopeLimitedByFiles(
    session,
    baseScope,
    JvmAndBuiltinsDeserializedContainerSourceProvider,
    isFallbackDependenciesProvider,
) { file ->
    val extension = file.extension
    extension == JavaClassFileType.INSTANCE.defaultExtension || extension == BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION
}

fun createStubBasedFirSymbolProviderForCommonMetadataFiles(
    session: LLFirSession,
    baseScope: GlobalSearchScope,
    isFallbackDependenciesProvider: Boolean,
): FirSymbolProvider = createStubBasedFirSymbolProviderForScopeLimitedByFiles(
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

fun createStubBasedFirSymbolProviderForKotlinNativeMetadataFiles(
    session: LLFirSession,
    baseScope: GlobalSearchScope,
    isFallbackDependenciesProvider: Boolean,
): FirSymbolProvider = createStubBasedFirSymbolProviderForScopeLimitedByFiles(
    session,
    baseScope,
    NullDeserializedContainerSourceProvider,
    isFallbackDependenciesProvider,
) { file -> file.extension == KLIB_METADATA_FILE_EXTENSION }

fun createStubBasedFirSymbolProviderForScopeLimitedByFiles(
    session: LLFirSession,
    baseScope: GlobalSearchScope,
    deserializedContainerSourceProvider: DeserializedContainerSourceProvider,
    isFallbackDependenciesProvider: Boolean,
    fileFilter: (VirtualFile) -> Boolean,
): StubBasedFirDeserializedSymbolProvider {
    return createFirSymbolProviderForScopeLimitedByFiles(
        session.project, baseScope, fileFilter,
        symbolProviderFactory = { reducedScope: GlobalSearchScope ->
            StubBasedFirDeserializedSymbolProvider(
                session,
                deserializedContainerSourceProvider,
                reducedScope,
                isFallbackDependenciesProvider,
            )
        }
    )
}

fun <T : FirSymbolProvider> createFirSymbolProviderForScopeLimitedByFiles(
    project: Project,
    baseScope: GlobalSearchScope,
    fileFilter: (VirtualFile) -> Boolean,
    symbolProviderFactory: (reducedScope: GlobalSearchScope) -> T,
): T {
    val scopeWithFileFiltering = object : DelegatingGlobalSearchScope(project, baseScope) {
        override fun contains(file: VirtualFile): Boolean {
            if (!fileFilter(file)) {
                return false
            }
            return super.contains(file)
        }
    }

    return symbolProviderFactory(scopeWithFileFiltering)
}
