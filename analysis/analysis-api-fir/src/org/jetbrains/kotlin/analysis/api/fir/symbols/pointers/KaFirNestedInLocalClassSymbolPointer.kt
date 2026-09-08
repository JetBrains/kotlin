/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBaseCachedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.scopes.getClassifiers
import org.jetbrains.kotlin.fir.scopes.impl.nestedClassifierScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.name.Name
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

internal class KaFirNestedInLocalClassSymbolPointer<T : KaClassLikeSymbol>(
    private val containingClassPointer: KaSymbolPointer<KaNamedClassSymbol>,
    private val name: Name,
    private val firOrigin: FirDeclarationOrigin,
    private val expectedClass: KClass<T>,
    originalSymbol: T?,
) : KaBaseCachedSymbolPointer<T>(originalSymbol) {

    @KaImplementationDetail
    override fun restoreIfNotCached(analysisSession: KaSession): T? {
        require(analysisSession is KaFirSession)
        val containingKaSymbol = containingClassPointer.restoreSymbol(analysisSession) ?: return null
        val containingFir = containingKaSymbol.firSymbol.fir as? FirRegularClass ?: return null

        return analysisSession.firSession.nestedClassifierScope(containingFir)
            ?.getClassifiers(name)
            ?.firstNotNullOfOrNull { kaSymbolIfApplicable(it, analysisSession) }
    }

    private fun kaSymbolIfApplicable(symbol: FirClassifierSymbol<*>, analysisSession: KaFirSession): T? {
        if (symbol !is FirClassLikeSymbol || symbol.origin != firOrigin) return null

        val classLikeSymbol = analysisSession.firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(symbol)
        return expectedClass.safeCast(classLikeSymbol)
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = other === this ||
            other is KaFirNestedInLocalClassSymbolPointer &&
            other.name == name &&
            other.firOrigin == firOrigin &&
            other.expectedClass == expectedClass &&
            other.containingClassPointer.pointsToTheSameSymbolAs(containingClassPointer)
}
