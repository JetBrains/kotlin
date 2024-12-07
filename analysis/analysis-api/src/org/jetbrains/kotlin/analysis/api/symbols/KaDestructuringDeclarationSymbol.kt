/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility

/**
 * A [KaSymbol] created from a [destructuring declaration][org.jetbrains.kotlin.psi.KtDestructuringDeclaration] (possibly from a lambda parameter).
 *
 * #### Examples
 *
 * - `val (a, _) = Pair(1, 2)` leads to `KaDestructuringDeclarationSymbol(entries = [a, _])`
 * - `Pair(1, _).let { (a, b) -> }` leads to `KaDestructuringDeclarationSymbol(entries = [a, _])`
 */
public abstract class KaDestructuringDeclarationSymbol : KaDeclarationSymbol {
    /**
     * A list of [KaVariableSymbol]s which were created from this destructuring declaration.
     *
     * The entries are usually [KaLocalVariableSymbol]s. However, for top-level destructuring declarations in scripts, the entries are
     * [KaKotlinPropertySymbol]s instead.
     *
     * #### Example
     *
     * ```
     * data class X(val y: Int, val z: String)
     * fun foo() {
     *      val (a, _) = x // the destruction
     * }
     * ```
     *
     * For the code above, the following symbols will be created (pseudocode):
     *
     * ```
     * val a: Int
     * val _: String
     * ```
     */
    public abstract val entries: List<KaVariableSymbol>

    final override val location: KaSymbolLocation get() = withValidityAssertion { KaSymbolLocation.LOCAL }

    @KaExperimentalApi
    final override val compilerVisibility: Visibility get() = withValidityAssertion { Visibilities.Local }
    final override val modality: KaSymbolModality get() = withValidityAssertion { KaSymbolModality.FINAL }

    final override val isActual: Boolean get() = withValidityAssertion { false }
    final override val isExpect: Boolean get() = withValidityAssertion { false }

    abstract override fun createPointer(): KaSymbolPointer<KaDestructuringDeclarationSymbol>
}
