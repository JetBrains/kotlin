/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.factory.configuration

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaModulePlatformKind
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.toModulePlatformKind
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirBuiltinsAndCloneableSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirDanglingFileSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.platform.TargetPlatform

/**
 * A configuration covering platform-specific options of a specific [module platform kind][KaModulePlatformKind].
 *
 * Each configuration is specific to a single module platform kind, such as [KaModulePlatformKind.JVM] or [KaModulePlatformKind.WASM]. In
 * case of a [metadata][KaModulePlatformKind.METADATA] module (having multiple concrete platforms such as JS *and* Wasm), a metadata
 * configuration is used instead of a platform-specific one. Hence, the configuration is *not* composable, unlike
 * [LLPlatformSessionComponentRegistration][org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.factory.components.LLPlatformSessionComponentRegistration].
 */
internal interface LLPlatformSessionConfiguration {
    /**
     * Creates the [FirKotlinScopeProvider] for source sessions.
     *
     * The function can be overridden to configure platform-specific scope wrapping (e.g., JVM mapped scopes).
     */
    fun createSourceScopeProvider(): FirKotlinScopeProvider = FirKotlinScopeProvider()

    /**
     * Create the [FirKotlinScopeProvider] for built-in sessions.
     *
     * The behavior is similar to the [createSourceScopeProvider].
     */
    fun createBuiltinsScopeProvider(): FirKotlinScopeProvider = FirKotlinScopeProvider()

    /**
     * Creates additional, platform-specific symbol providers to include in the session's composite [FirSymbolProvider].
     *
     * For dangling file sessions, [createPlatformSpecificSymbolProvidersForDanglingFileSession] is called instead.
     *
     * For binary library sessions, [createBinaryLibrarySymbolProviders] is called instead.
     */
    fun createPlatformSpecificSymbolProviders(
        session: LLFirSession,
        contentScope: GlobalSearchScope,
    ): List<FirSymbolProvider> = emptyList()

    /**
     * Creates additional, platform-specific symbol providers to include in the dangling file session's composite [FirSymbolProvider].
     *
     * In contrast to [createPlatformSpecificSymbolProviders], this function receives the [contextSession] of the dangling file's context
     * module, from which symbol providers might be reused or adapted.
     */
    fun createPlatformSpecificSymbolProvidersForDanglingFileSession(
        session: LLFirDanglingFileSession,
        contextSession: LLFirSession,
    ): List<FirSymbolProvider> = emptyList()

    /**
     * Creates additional, platform-specific symbol providers to include in the built-in sessions.
     *
     * Note that unlike source and library sessions, sessions for built-in modules are cached more aggressively on a project level.
     *
     * @see org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.factory.LLFirBuiltinsSessionFactory
     */
    fun createPlatformSpecificSymbolProvidersForBuiltinsSession(
        session: LLFirBuiltinsAndCloneableSession
    ): List<FirSymbolProvider> = emptyList()

    /**
     * Creates the symbol providers for binary library sessions.
     *
     * In contrast to the platform-specific endpoints above which create additional symbol providers, this function determines the full
     * list of symbol providers for the binary library session's own providers.
     */
    fun createBinaryLibrarySymbolProviders(session: LLFirSession, scope: GlobalSearchScope): List<FirSymbolProvider>

    companion object {
        fun forPlatform(targetPlatform: TargetPlatform, project: Project): LLPlatformSessionConfiguration =
            when (targetPlatform.toModulePlatformKind()) {
                KaModulePlatformKind.METADATA -> LLMetadataSessionConfiguration(project)
                KaModulePlatformKind.JVM -> LLJvmSessionConfiguration(project)
                KaModulePlatformKind.JS -> LLJsSessionConfiguration(project)
                KaModulePlatformKind.WASM -> LLWasmSessionConfiguration(project)
                KaModulePlatformKind.NATIVE -> LLNativeSessionConfiguration(project)
            }
    }
}
