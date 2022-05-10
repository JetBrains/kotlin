/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.buildSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createSignature
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.psi.KtElement


class KtAnalysisSessionFe10BindingHolder private constructor(
    internal val firAnalysisSession: KtFirAnalysisSession
) {
    val analysisSession: KtAnalysisSession get() = firAnalysisSession

    val firResolveSession: LLFirResolveSession get() = firAnalysisSession.firResolveSession

    fun buildClassLikeSymbol(fir: FirClassLikeDeclaration): KtClassLikeSymbol =
        firAnalysisSession.firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(fir.symbol)

    fun buildKtType(coneType: FirTypeRef): KtType =
        firAnalysisSession.firSymbolBuilder.typeBuilder.buildKtType(coneType)

    fun buildSymbol(firElement: FirElement): KtSymbol? = firElement.buildSymbol(firAnalysisSession.firSymbolBuilder)

    @Suppress("UNCHECKED_CAST", "NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    inline fun <T : FirDeclaration, R> withFir(ktSymbol: KtSymbol, crossinline action: (T) -> R) =
        (ktSymbol as KtFirSymbol<*>).firSymbol.fir.let { action(it as T) }

    fun toSignature(ktSymbol: KtSymbol): IdSignature = (ktSymbol as KtFirSymbol<*>).firSymbol.createSignature()

    companion object {
        fun create(firResolveSession: LLFirResolveSession, token: KtLifetimeToken, @Suppress("UNUSED_PARAMETER") ktElement: KtElement): KtAnalysisSessionFe10BindingHolder {
            @Suppress("DEPRECATION")
            val firAnalysisSession = KtFirAnalysisSession.createAnalysisSessionByFirResolveSession(firResolveSession, token)
            return KtAnalysisSessionFe10BindingHolder(firAnalysisSession)
        }
    }
}
