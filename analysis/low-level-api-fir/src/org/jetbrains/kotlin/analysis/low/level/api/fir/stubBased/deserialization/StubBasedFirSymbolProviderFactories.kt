/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInFileType
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.library.KLIB_METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol

internal fun createStubBasedFirSymbolProviderForClassFiles(
    project: Project,
    baseScope: GlobalSearchScope,
    session: FirSession,
    moduleDataProvider: SingleModuleDataProvider,
    kotlinScopeProvider: FirKotlinScopeProvider,
): FirSymbolProvider = createStubBasedFirSymbolProviderForScopeLimitedByFiles(
    project, baseScope, session, moduleDataProvider, kotlinScopeProvider,
    fileFilter = { file ->
        val extension = file.extension
        extension == JavaClassFileType.INSTANCE.defaultExtension || extension == BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION
    },
)

internal fun createStubBasedFirSymbolProviderForCommonMetadataFiles(
    project: Project,
    baseScope: GlobalSearchScope,
    session: FirSession,
    moduleDataProvider: SingleModuleDataProvider,
    kotlinScopeProvider: FirKotlinScopeProvider,
): FirSymbolProvider = createStubBasedFirSymbolProviderForScopeLimitedByFiles(
    project, baseScope, session, moduleDataProvider, kotlinScopeProvider,
    fileFilter = { file -> file.fileType == KotlinBuiltInFileType },
)

internal fun createStubBasedFirSymbolProviderForKotlinNativeMetadataFiles(
    project: Project,
    baseScope: GlobalSearchScope,
    session: FirSession,
    moduleDataProvider: SingleModuleDataProvider,
    kotlinScopeProvider: FirKotlinScopeProvider,
): FirSymbolProvider = createStubBasedFirSymbolProviderForScopeLimitedByFiles(
    project, baseScope, session, moduleDataProvider, kotlinScopeProvider,
    fileFilter = { file -> file.extension == KLIB_METADATA_FILE_EXTENSION },
)

internal fun createStubBasedFirSymbolProviderForScopeLimitedByFiles(
    project: Project,
    baseScope: GlobalSearchScope,
    session: FirSession,
    moduleDataProvider: SingleModuleDataProvider,
    kotlinScopeProvider: FirKotlinScopeProvider,
    fileFilter: (VirtualFile) -> Boolean,
): StubBasedFirDeserializedSymbolProvider {
    return createFirSymbolProviderForScopeLimitedByFiles(
        project, baseScope, fileFilter,
        symbolProviderFactory = { reducedScope: GlobalSearchScope ->
            StubBasedFirDeserializedSymbolProvider(
                session, moduleDataProvider, kotlinScopeProvider, project, reducedScope, FirDeclarationOrigin.Library,
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
        override fun contains(file: VirtualFile): Boolean {
            if (!fileFilter(file)) {
                return false
            }
            return super.contains(file)
        }
    }

    return symbolProviderFactory(scopeWithFileFiltering)
}
