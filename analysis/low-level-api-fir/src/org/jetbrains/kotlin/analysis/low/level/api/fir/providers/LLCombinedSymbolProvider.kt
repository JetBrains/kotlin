/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider

/**
 * A symbol provider which combines multiple individual symbol providers of the same type. Combined symbol providers typically have an
 * advantage over naively querying the list of individual symbol providers, such as caching or a single index access with a combined scope.
 */
abstract class LLCombinedSymbolProvider<P : FirSymbolProvider>(session: FirSession) : FirSymbolProvider(session) {
    abstract val providers: List<P>

    /**
     * Estimates the number of symbols contained in the combined symbol provider's own caches. The metric does not include the cache sizes
     * of the individual symbol providers.
     *
     * The purpose of this metric is to estimate the current **cache overhead** of a combined symbol provider.
     */
    abstract fun estimateSymbolCacheSize(): Long
}
