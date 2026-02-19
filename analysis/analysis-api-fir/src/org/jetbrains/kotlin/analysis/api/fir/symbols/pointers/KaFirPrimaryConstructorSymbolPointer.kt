/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBaseCachedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.isJavaOrEnhancement
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.getPrimaryConstructorSymbol

/** @see KaFirSecondaryConstructorSymbolPointer */
internal class KaFirPrimaryConstructorSymbolPointer(
    private val ownerPointer: KaSymbolPointer<KaClassSymbol>,
    originalSymbol: KaConstructorSymbol?,
) : KaBaseCachedSymbolPointer<KaConstructorSymbol>(originalSymbol = originalSymbol) {
    @KaImplementationDetail
    override fun restoreIfNotCached(analysisSession: KaSession): KaConstructorSymbol? {
        require(analysisSession is KaFirSession)

        val ownerSymbol = with(analysisSession) {
            ownerPointer.restoreSymbol()
        }

        val firSymbol = ownerSymbol?.firSymbol
        val constructorSymbol = if (firSymbol?.origin is FirDeclarationOrigin.Java) {
            val session = analysisSession.firSession
            // This is required to trigger Java enhancement
            firSymbol.getPrimaryConstructorSymbol(session, analysisSession.getScopeSessionFor(session))
        } else {
            ownerSymbol?.firSymbol?.primaryConstructorIfAny(analysisSession.firSession)
        } ?: return null

        return analysisSession.firSymbolBuilder.functionBuilder.buildConstructorSymbol(constructorSymbol)
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFirPrimaryConstructorSymbolPointer &&
            other.ownerPointer.pointsToTheSameSymbolAs(ownerPointer)
}
