/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

/**
 * A [KtFirSymbol] that is backed by some [PsiElement] and builds [firSymbol] lazily (by convention), allowing some properties to be
 * calculated without the need to build a [firSymbol].
 */
internal interface KtFirPsiSymbol<P : PsiElement, out S : FirBasedSymbol<*>> : KtFirSymbol<S> {
    override val psi: P

    /**
     * The origin should be provided without using [firSymbol], if possible.
     */
    abstract override val origin: KtSymbolOrigin
}
