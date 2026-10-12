/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbols.id

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.id.FirSymbolId
import org.jetbrains.kotlin.fir.symbols.id.FirSymbolIdFactory
import org.jetbrains.kotlin.fir.symbols.id.FirUniqueSymbolId

/**
 * Creates source-based symbol IDs for LL FIR sessions if [isSourceBasedEnabled] is set. Otherwise, all symbol IDs are unique.
 */
internal class LLSymbolIdFactory(val session: LLFirSession, private val isSourceBasedEnabled: Boolean) : FirSymbolIdFactory() {
    override fun <E : FirDeclaration, S : FirBasedSymbol<E>> unique(): FirSymbolId<S> = FirUniqueSymbolId()

    @OptIn(SuspiciousFakeSourceCheck::class)
    override fun <E : FirDeclaration, S : FirBasedSymbol<E>> sourceBased(sourceElement: KtSourceElement): FirSymbolId<S> {
        if (!isSourceBasedEnabled) return unique()

        require(sourceElement is KtPsiSourceElement) {
            "Source-based symbol IDs in the Analysis API can only be created from PSI source elements, but not from" +
                    " `${sourceElement::class.simpleName}`."
        }

        return when (sourceElement) {
            is KtRealPsiSourceElement -> psiBased(sourceElement.psi)
            is KtFakePsiSourceElement -> LLFakePsiSymbolId(session, sourceElement)
        }
    }

    internal fun <E : FirDeclaration, S : FirBasedSymbol<E>> psiBased(psi: PsiElement): FirSymbolId<S> {
        if (!isSourceBasedEnabled) return unique()

        return LLRealPsiSymbolId(session, psi)
    }
}
