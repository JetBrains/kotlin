/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbols.id

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakePsiSourceElement
import org.jetbrains.kotlin.SuspiciousFakeSourceCheck
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.id.FirSymbolId
import org.jetbrains.kotlin.psi.KtElement

// TODO (marco): This is a hack for receiver type symbols! It should be removed later.
@OptIn(SuspiciousFakeSourceCheck::class)
internal class LLNonRestorableRealPsiSymbolId<S : FirBasedSymbol<*>>(private val psi: PsiElement) : FirSymbolId<S>() {
    private lateinit var _symbol: S

    override val symbol: S
        get() = _symbol

    @FirImplementationDetail
    override fun bind(symbol: S) {
        _symbol = symbol
    }

    override fun equals(other: Any?): Boolean = other is LLNonRestorableRealPsiSymbolId<*> && psi == other.psi

    override fun hashCode(): Int = psi.hashCode()
}
