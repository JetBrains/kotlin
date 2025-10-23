/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbols.id

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.id.FirSymbolId
import org.jetbrains.kotlin.fir.symbols.id.FirSymbolIdFactory
import org.jetbrains.kotlin.fir.symbols.id.FirUniqueSymbolId
import org.jetbrains.kotlin.fir.symbols.id.symbolIdFactory
import org.jetbrains.kotlin.psi.KtContextReceiver
import org.jetbrains.kotlin.psi.KtTypeReference

internal class LLSymbolIdFactory(val session: LLFirSession) : FirSymbolIdFactory() {
    override fun <E : FirDeclaration, S : FirBasedSymbol<E>> unique(): FirSymbolId<S> = FirUniqueSymbolId()

    @OptIn(SuspiciousFakeSourceCheck::class)
    override fun <E : FirDeclaration, S : FirBasedSymbol<E>> sourceBased(sourceElement: KtSourceElement): FirSymbolId<S> {
        require(sourceElement is KtPsiSourceElement) {
            "Source-based symbol IDs in the Analysis API can only be created from PSI source elements, but not from" +
                    " `${sourceElement::class.simpleName}`."
        }

        return when (sourceElement) {
            is KtRealPsiSourceElement -> psiBased(sourceElement.psi)
            is KtFakePsiSourceElement -> LLFakePsiSymbolId(sourceElement)
        }
    }

    fun <E : FirDeclaration, S : FirBasedSymbol<E>> psiBased(psi: PsiElement): FirSymbolId<S> =
        when (psi) {
            // TODO (marco): It would be better to have the default, real PSI symbol IDs here and support restoration specifically
            //  of these special cases. Otherwise, symbol references to such symbols will keep the symbol itself alive.
            is KtTypeReference, is KtContextReceiver -> LLNonRestorableRealPsiSymbolId(psi)

            else -> LLRealPsiSymbolId(session, psi)
        }
}

internal val FirSession.llSymbolIdFactory: LLSymbolIdFactory
    get() = symbolIdFactory as LLSymbolIdFactory
