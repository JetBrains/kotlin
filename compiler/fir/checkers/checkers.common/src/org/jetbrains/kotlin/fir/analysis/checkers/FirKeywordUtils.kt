/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtNodeTypeId
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.SyntaxId
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals

// DO
// - use this to retrieve modifiers on the source and confirm a certain modifier indeed appears
// DON'T
// - don't use this to report an error or warning *on* that specific modifier. Use positioning strategies instead.
abstract class FirModifierList {
    abstract val modifiers: List<FirModifier>

    abstract fun getAnnotationSources(): List<KtSourceElement>

    operator fun get(tokenId: SyntaxId): FirModifier? = modifiers.firstOrNull { it.tokenId == tokenId }

    operator fun contains(tokenId: SyntaxId): Boolean = modifiers.any { it.tokenId == tokenId }
}

/**
 * @property tokenId The ID of the modifier keyword (one of the `*_MODIFIER` IDs from [org.jetbrains.kotlin.KtTokenId]).
 */
abstract class FirModifier(val tokenId: SyntaxId) {
    /**
     * The keyword text of the modifier (e.g., `public`) to be used in diagnostic messages.
     */
    abstract val tokenName: String

    abstract val source: KtSourceElement
}

operator fun FirModifierList?.contains(tokenId: SyntaxId): Boolean = this?.contains(tokenId) == true

fun FirElement.getModifierList(): FirModifierList? = when {
    source?.kind is KtFakeSourceElementKind.DelegatedPropertyAccessor && source?.syntaxId != KtNodeTypeId.PROPERTY_ACCESSOR -> null
    else -> source.getModifierList()
}

fun FirElement.getModifier(tokenId: SyntaxId): FirModifier? = getModifierList()?.get(tokenId)

fun FirElement.hasModifier(tokenId: SyntaxId): Boolean = tokenId in getModifierList()

@OptIn(SymbolInternals::class)
fun FirBasedSymbol<*>.hasModifier(tokenId: SyntaxId): Boolean = fir.hasModifier(tokenId)

internal enum class ValOrVarKeyword(val keyword: String) {
    VAL("val"),
    VAR("var")
}
