/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

/**
 * A [KaFirSymbol] that is possibly backed by some [PsiElement] and builds [firSymbol] lazily (by convention),
 * allowing some properties to be calculated without the need to build a [firSymbol].
 */
internal interface KaFirPsiSymbol<P : PsiElement, out S : FirBasedSymbol<*>> : KaFirSymbol<S> {
    /**
     * The [PsiElement] which can be used as a source of truth for some other property implementations.
     */
    val backingPsi: P?

    /**
     * The lazy implementation of [FirBasedSymbol].
     *
     * The implementation is either built on top of [backingPsi] or provided during creation.
     *
     * @see firSymbol
     */
    val lazyFirSymbol: Lazy<S>

    /**
     * The origin should be provided without using [firSymbol], if possible.
     */
    abstract override val origin: KaSymbolOrigin

    override val firSymbol: S get() = lazyFirSymbol.value
}
