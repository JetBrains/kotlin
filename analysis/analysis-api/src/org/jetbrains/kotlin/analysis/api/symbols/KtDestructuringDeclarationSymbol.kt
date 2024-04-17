/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer

/**
 *  A [KtSymbol] created from a [destructuring declaration][org.jetbrains.kotlin.psi.KtDestructuringDeclaration] (possibly from a lambda parameter).
 *
 * Examples:
 * - `val (a, _) = Pair(1, 2)` leads to `KtDestructuringDeclarationSymbol(entries = [a, _])`
 * - `Pair(1, _).let { (a, b) -> }` leads to `KtDestructuringDeclarationSymbol(entries = [a, _])`
 */
public abstract class KtDestructuringDeclarationSymbol : KtDeclarationSymbol, KtSymbolWithKind {
    final override val symbolKind: KtSymbolKind get() = withValidityAssertion { KtSymbolKind.LOCAL }
    final override val typeParameters: List<KtTypeParameterSymbol> get() = withValidityAssertion { emptyList() }

    /**
     * A list of [KtLocalVariableSymbol]s which were created from this destructuring declaration.
     *
     * E.g., for the following code:
     * ```
     * data class X(val y: Int, val z: String)
     * fun foo() {
     *      val (a, _) = x // the destruction
     * }
     * ```
     *
     * the following symbols will be created (pseudocode)
     * ```
     * val a: Int
     * val _: String
     * ```
     */
    public abstract val entries: List<KtLocalVariableSymbol>

    context(KtAnalysisSession)
    abstract override fun createPointer(): KtSymbolPointer<KtDestructuringDeclarationSymbol>
}