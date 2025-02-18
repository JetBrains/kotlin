/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.KotlinDeserializedDeclarationsOrigin
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformSettings
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization.LLStubBasedLibrarySymbolProviderFactory
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider

/**
 * [LLLibrarySymbolProviderFactory] creates symbol providers in accordance with [KotlinPlatformSettings.deserializedDeclarationsOrigin].
 * Its implementations should be lightweight as the factory is neither a service nor cached.
 */
interface LLLibrarySymbolProviderFactory {
    fun createJvmLibrarySymbolProvider(
        session: LLFirSession,
        firJavaFacade: FirJavaFacade,
        packagePartProvider: PackagePartProvider,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider>

    fun createCommonLibrarySymbolProvider(
        session: LLFirSession,
        packagePartProvider: PackagePartProvider,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider>

    fun createNativeLibrarySymbolProvider(
        session: LLFirSession,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider>

    fun createJsLibrarySymbolProvider(
        session: LLFirSession,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider>

    fun createWasmLibrarySymbolProvider(
        session: LLFirSession,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider>

    fun createBuiltinsSymbolProvider(session: LLFirSession): List<FirSymbolProvider>

    companion object {
        fun fromSettings(project: Project): LLLibrarySymbolProviderFactory {
            val platformSettings = KotlinPlatformSettings.getInstance(project)
            return when (platformSettings.deserializedDeclarationsOrigin) {
                KotlinDeserializedDeclarationsOrigin.BINARIES -> LLBinaryOriginLibrarySymbolProviderFactory
                KotlinDeserializedDeclarationsOrigin.STUBS -> LLStubBasedLibrarySymbolProviderFactory
            }
        }
    }
}
