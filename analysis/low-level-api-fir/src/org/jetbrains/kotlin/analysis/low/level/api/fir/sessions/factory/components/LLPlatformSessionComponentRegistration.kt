/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.factory.components

import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.forEachComponentPlatform
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.DefaultImportsProvider

/**
 * Handles the registration of platform-specific session components.
 *
 * The interface should be implemented once per target platform (JVM, JS, Wasm, Native). Registrations are composable, so multiple component
 * registrations can be applied to the same session (for metadata sessions covering multiple platforms).
 */
internal interface LLPlatformSessionComponentRegistration {
    /**
     * Registers components that apply to all platform-aware session kinds (sources, resolvable libraries, binary libraries, dangling
     * files).
     *
     * [platformSpecificSymbolProviders] contains platform-specific symbol providers that are already part of the session's symbol provider,
     * but might have to be registered as platform-specific components, too. For binary library sessions, this list is empty, as their
     * symbol providers are created in a non-standard way.
     */
    fun registerComponents(session: LLFirSession, platformSpecificSymbolProviders: List<FirSymbolProvider>)

    /**
     * Registers components specific to source sessions.
     */
    fun registerSourceComponents(session: LLFirSession) {}

    /**
     * Registers components specific to resolvable library sessions.
     */
    fun registerResolvableLibraryComponents(session: LLFirSession) {}

    /**
     * Registers components specific to binary library sessions.
     */
    fun registerBinaryLibraryComponents(session: LLFirSession) {}

    /**
     * Registers components specific to dangling file sessions.
     */
    fun registerDanglingFileComponents(session: LLFirSession) {}

    /**
     * The [DefaultImportsProvider] that is registered by [registerComponents] for this platform.
     *
     * The property is needed to retrieve the [DefaultImportsProvider] for a [TargetPlatform] without actually setting up a session.
     */
    val defaultImportsProvider: DefaultImportsProvider

    companion object {
        fun forPlatform(targetPlatform: TargetPlatform): List<LLPlatformSessionComponentRegistration> =
            buildList {
                targetPlatform.forEachComponentPlatform(
                    onJvm = { add(LLJvmSessionComponentRegistration) },
                    onJs = { add(LLJsSessionComponentRegistration) },
                    onWasm = { add(LLWasmSessionComponentRegistration(it)) },
                    onNative = { add(LLNativeSessionComponentRegistration) },
                )
            }
    }
}
