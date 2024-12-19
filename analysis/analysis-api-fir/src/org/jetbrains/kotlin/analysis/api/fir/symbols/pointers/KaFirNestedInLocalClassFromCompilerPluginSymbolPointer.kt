/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBaseCachedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.scopes.getClassifiers
import org.jetbrains.kotlin.fir.scopes.impl.nestedClassifierScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.Name
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

internal class KaFirNestedInLocalClassFromCompilerPluginSymbolPointer(
    private val containingClassPointer: KaSymbolPointer<KaNamedClassSymbol>,
    private val name: Name,
    private val compilerPluginOrigin: GeneratedDeclarationKey,
    originalSymbol: KaNamedClassSymbol?,
) : KaBaseCachedSymbolPointer<KaNamedClassSymbol>(originalSymbol) {

    @KaImplementationDetail
    override fun restoreIfNotCached(analysisSession: KaSession): KaNamedClassSymbol? {
        require(analysisSession is KaFirSession)
        val containingKaSymbol = containingClassPointer.restoreSymbol(analysisSession) ?: return null
        val containingFir = containingKaSymbol.firSymbol.fir as? FirRegularClass ?: return null

        val firForCreatedSymbol = analysisSession.firSession.nestedClassifierScope(containingFir)
            ?.getClassifiers(name)
            ?.firstNotNullOfOrNull { if (isApplicableCandidate(it)) it else null }
            ?: return null

        return analysisSession.firSymbolBuilder.classifierBuilder.buildNamedClassSymbol(firForCreatedSymbol)
    }

    @OptIn(ExperimentalContracts::class)
    private fun isApplicableCandidate(symbol: FirClassifierSymbol<*>): Boolean {
        contract {
            returns(true) implies (symbol is FirRegularClassSymbol)
        }
        if (symbol !is FirRegularClassSymbol) return false
        val pluginOrigin = symbol.origin as? FirDeclarationOrigin.Plugin ?: return false
        return pluginOrigin.key == compilerPluginOrigin
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = other === this ||
            other is KaFirNestedInLocalClassFromCompilerPluginSymbolPointer &&
            other.name == name &&
            other.compilerPluginOrigin == compilerPluginOrigin &&
            other.containingClassPointer.pointsToTheSameSymbolAs(containingClassPointer)
}
