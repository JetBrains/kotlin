/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.declarations.roots

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirLibraryLikeSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLKotlinStubBasedLibrarySymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLModuleWithDependenciesSymbolProvider
import org.jetbrains.kotlin.fir.caches.FirCacheInternals
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider

/**
 * Collects all cached FIR *root declarations* contained in [this] [LLFirSession] that are subject to **consistency checks:**
 *
 * - **Source** cache roots: the FIR files currently cached by a resolvable module session.
 * - **Library** cache roots: the top-level declarations currently cached by the session's [LLKotlinStubBasedLibrarySymbolProvider]s.
 *
 * Declarations from dependency sessions are not included.
 *
 * Due to its heaviness, the function should only be used in tests. It can also be used in temporary assertions during development, for
 * which it resides in production sources.
 */
@TestOnly
@OptIn(FirCacheInternals::class)
internal fun LLFirSession.collectCachedCheckableRootDeclarations(): List<FirDeclaration> =
    buildList {
        val session = this@collectCachedCheckableRootDeclarations

        if (session is LLFirResolvableModuleSession) {
            addAll(session.moduleComponents.cache.getAllCachedFirFiles())
        }

        if (session is LLFirLibraryLikeSession) {
            val symbolProviders = (session.symbolProvider as? LLModuleWithDependenciesSymbolProvider)?.providers

            symbolProviders
                ?.filterIsInstance<LLKotlinStubBasedLibrarySymbolProvider>()
                ?.forEach { addAll(it.cachedDeclarations) }
        }
    }
