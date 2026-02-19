/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBaseCachedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.FirCallableSignature
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expandedClassWithConstructorsScope
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol

/**
 * A pointer to restore a typealiased [KaConstructorSymbol].
 *
 * This pointer is really similar to [KaFirTypeAliasedConstructorMemberPointer];
 * however, we cannot directly reuse it at the moment, because [KaTypeAliasSymbol]
 * does not implement [org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol].
 *
 * In the future (probably when KT-73046 is going to be implemented), this might change,
 * and this pointer could be fully replaced by [KaFirTypeAliasedConstructorMemberPointer].
 *
 * The structure of the code is intentionally made similar to make this move easier in the future.
 */
internal class KaFirTypeAliasedConstructorMemberPointer(
    private val ownerPointer: KaSymbolPointer<KaTypeAliasSymbol>,
    private val signature: FirCallableSignature,
    originalSymbol: KaConstructorSymbol?,
) : KaBaseCachedSymbolPointer<KaConstructorSymbol>(originalSymbol) {
    override fun restoreIfNotCached(analysisSession: KaSession): KaConstructorSymbol? {
        require(analysisSession is KaFirSession)
        val scope = with(analysisSession) {
            val ownerSymbol = ownerPointer.restoreSymbol() ?: return null
            val owner = ownerSymbol.firSymbol
            getSearchScope(analysisSession, owner)
        } ?: return null

        return analysisSession.chooseCandidateAndCreateSymbol(scope)
    }

    private fun KaFirSession.chooseCandidateAndCreateSymbol(candidates: FirScope): KaConstructorSymbol? {
        val firConstructor = candidates.findDeclarationWithSignature<FirConstructor>(signature) {
            processDeclaredConstructors(it)
        } ?: return null

        return firSymbolBuilder.functionBuilder.buildConstructorSymbol(firConstructor.symbol)
    }

    private fun getSearchScope(analysisSession: KaFirSession, typeAlias: FirTypeAliasSymbol): FirScope? {
        val firSession = analysisSession.firSession
        val scopeSession = analysisSession.getScopeSessionFor(firSession)

        val (_, typeAliasConstructorsSubstitutingScope) = typeAlias.expandedClassWithConstructorsScope(
            firSession,
            scopeSession,
            memberRequiredPhaseForRegularClasses = FirResolvePhase.STATUS
        ) ?: return null

        return typeAliasConstructorsSubstitutingScope
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = other === this ||
            other is KaFirTypeAliasedConstructorMemberPointer &&
            other.signature == signature &&
            other.ownerPointer.pointsToTheSameSymbolAs(ownerPointer)
}