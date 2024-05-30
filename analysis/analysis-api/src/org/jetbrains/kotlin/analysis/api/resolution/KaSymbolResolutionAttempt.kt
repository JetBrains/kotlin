/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolution

import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol

public sealed interface KaSymbolResolutionAttempt : KaLifetimeOwner

public interface KaSymbolResolutionSuccess : KaSymbolResolutionAttempt {
    public val symbol: KaSymbol
}

public interface KaSymbolResolutionError : KaSymbolResolutionAttempt {
    public val diagnostic: KaDiagnostic
    public val candidateSymbols: List<KaSymbol>
}

/**
 * Returns a list of [KaSymbol].
 *
 * - If [this] is an instance of [KaSymbolResolutionSuccess], the list will contain only [KaSymbolResolutionSuccess.symbol].
 * - If [this] is an instance of [KaSymbolResolutionError], the list will contain [KaSymbolResolutionError.candidateSymbols].
 *
 * @return the list of [KaSymbol]
 */
public val KaSymbolResolutionAttempt.symbols: List<KaSymbol>
    get() = when (this) {
        is KaSymbolResolutionSuccess -> listOf(symbol)
        is KaSymbolResolutionError -> candidateSymbols
    }
