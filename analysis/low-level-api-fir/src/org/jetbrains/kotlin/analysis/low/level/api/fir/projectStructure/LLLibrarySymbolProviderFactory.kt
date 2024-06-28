/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.KotlinDeserializedDeclarationsOrigin
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformSettings
import org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization.LLStubBasedLibrarySymbolProviderFactory
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider

/**
 * [LLLibrarySymbolProviderFactory] creates symbol providers in accordance with [KotlinPlatformSettings.deserializedDeclarationsOrigin].
 * Its implementations should be lightweight as the factory is neither a service nor cached.
 */
internal interface LLLibrarySymbolProviderFactory {
    fun createJvmLibrarySymbolProvider(
        session: FirSession,
        moduleData: LLFirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
        moduleDataProvider: SingleModuleDataProvider,
        firJavaFacade: FirJavaFacade,
        packagePartProvider: PackagePartProvider,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider>

    fun createCommonLibrarySymbolProvider(
        session: FirSession,
        moduleData: LLFirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
        moduleDataProvider: SingleModuleDataProvider,
        packagePartProvider: PackagePartProvider,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider>

    fun createNativeLibrarySymbolProvider(
        session: FirSession,
        moduleData: LLFirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
        moduleDataProvider: SingleModuleDataProvider,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider>

    fun createJsLibrarySymbolProvider(
        session: FirSession,
        moduleData: LLFirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
        moduleDataProvider: SingleModuleDataProvider,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider>

    fun createBuiltinsSymbolProvider(
        session: FirSession,
        moduleData: LLFirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
    ): List<FirSymbolProvider>

    companion object {
        fun fromSettings(project: Project): LLLibrarySymbolProviderFactory {
            val platformSettings = KotlinPlatformSettings.getInstance(project)
            return when (platformSettings.deserializedDeclarationsOrigin) {
                KotlinDeserializedDeclarationsOrigin.BINARIES -> LLBinaryOriginLibrarySymbolProviderFactory(project)
                KotlinDeserializedDeclarationsOrigin.STUBS -> LLStubBasedLibrarySymbolProviderFactory(project)
            }
        }
    }
}
