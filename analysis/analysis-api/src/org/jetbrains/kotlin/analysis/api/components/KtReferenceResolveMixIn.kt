/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KtSymbolBasedReference
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.references.KtReference

public interface KtReferenceResolveMixIn : KtAnalysisSessionMixIn {
    public fun KtReference.resolveToSymbols(): Collection<KtSymbol> {
        check(this is KtSymbolBasedReference) { "To get reference symbol the one should be KtSymbolBasedReference" }
        return analysisSession.resolveToSymbols()
    }

    public fun KtReference.resolveToSymbol(): KtSymbol? {
        check(this is KtSymbolBasedReference) { "To get reference symbol the one should be KtSymbolBasedReference but was ${this::class}" }
        return resolveToSymbols().singleOrNull()
    }
}