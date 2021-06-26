/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.utils

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.frontend.api.InvalidWayOfUsingAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.buildSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers.createSignature
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.psi.KtElement


class KtAnalysisSessionFe10BindingHolder private constructor(
    internal val firAnalysisSession: KtFirAnalysisSession
) {
    val analysisSession: KtAnalysisSession get() = firAnalysisSession

    val firResolveState: FirModuleResolveState get() = firAnalysisSession.firResolveState

    fun buildClassLikeSymbol(fir: FirClassLikeDeclaration): KtClassLikeSymbol =
        firAnalysisSession.firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(fir)

    fun buildKtType(coneType: FirTypeRef): KtType =
        firAnalysisSession.firSymbolBuilder.typeBuilder.buildKtType(coneType)

    fun buildSymbol(firElement: FirElement): KtSymbol? = firElement.buildSymbol(firAnalysisSession.firSymbolBuilder)

    @Suppress("UNCHECKED_CAST", "NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    inline fun <T : FirDeclaration, R> withFir(ktSymbol: KtSymbol, crossinline action: (T) -> R) =
        (ktSymbol as KtFirSymbol<T>).firRef.withFir(action = action)

    fun toSignature(ktSymbol: KtSymbol): IdSignature = (ktSymbol as KtFirSymbol<*>).firRef.withFir { it.createSignature() }

    companion object {
        @InvalidWayOfUsingAnalysisSession
        fun create(firResolveState: FirModuleResolveState, token: ValidityToken, ktElement: KtElement): KtAnalysisSessionFe10BindingHolder {
            val firAnalysisSession = KtFirAnalysisSession.createAnalysisSessionByResolveState(firResolveState, token, ktElement)
            return KtAnalysisSessionFe10BindingHolder(firAnalysisSession)
        }
    }
}
