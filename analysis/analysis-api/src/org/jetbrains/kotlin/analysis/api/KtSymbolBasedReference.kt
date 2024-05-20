/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api

import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.idea.references.KtReference

public interface KaSymbolBasedReference : KtReference {
    public fun KaSession.resolveToSymbols(): Collection<KaSymbol>
}

public typealias KtSymbolBasedReference = KaSymbolBasedReference