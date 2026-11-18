/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.syntax

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.syntax.SyntaxId

/**
 * The modifier list of a declaration, independent of the syntax tree it was built from.
 *
 * DO
 * - use this to retrieve modifiers on the source and confirm a certain modifier indeed appears
 * DON'T
 * - don't use this to report an error or warning *on* that specific modifier. Use positioning strategies instead.
 *
 * @property modifiers The modifier keywords in source order.
 * @property annotationSources The sources of the annotation entries in source order.
 */
class FirModifierList(
    val modifiers: List<FirModifier>,
    val annotationSources: List<KtSourceElement>,
) {
    operator fun get(tokenId: SyntaxId): FirModifier? = modifiers.firstOrNull { it.tokenId == tokenId }

    operator fun contains(tokenId: SyntaxId): Boolean = modifiers.any { it.tokenId == tokenId }
}

/**
 * @property tokenId The ID of the modifier keyword (one of the `*_MODIFIER` IDs from [org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds]).
 * @property tokenName The keyword text of the modifier (e.g., `public`) to be used in diagnostic messages.
 */
class FirModifier(
    val tokenId: SyntaxId,
    val tokenName: String,
    val source: KtSourceElement,
)
