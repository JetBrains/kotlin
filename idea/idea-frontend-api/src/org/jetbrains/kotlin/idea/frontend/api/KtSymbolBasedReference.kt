/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api

import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.references.KtSimpleReference

interface KtSymbolBasedReference : KtReference {
    fun resolveToSymbols(analysisSession: KtAnalysisSession): Collection<KtSymbol>
}

fun KtReference.resolveToSymbols(analysisSession: KtAnalysisSession): Collection<KtSymbol> {
    check(this is KtSymbolBasedReference) { "To get reference symbol the one should be KtSymbolBasedReference" }
    return resolveToSymbols(analysisSession)
}

fun KtSimpleReference<*>.resolveToSymbol(analysisSession: KtAnalysisSession): KtSymbol? {
    check(this is KtSymbolBasedReference) { "To get reference symbol the one should be KtSymbolBasedReference but was ${this::class}" }
    return resolveToSymbols(analysisSession).singleOrNull()
}