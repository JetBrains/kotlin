/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.withSymbolAttachment
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal abstract class KaFirMemberSymbolPointer<S : KaSymbol>(
    private val ownerPointer: KaSymbolPointer<KaDeclarationContainerSymbol>,
    private val isStatic: Boolean = false,
) : KaSymbolPointer<S>() {
    @KaImplementationDetail
    final override fun restoreSymbol(analysisSession: KaSession): S? {
        require(analysisSession is KaFirSession)
        val scope = with(analysisSession) {
            val ownerSymbol = ownerPointer.restoreSymbol() ?: return null
            val owner = ownerSymbol.firSymbol as? FirClassSymbol ?: return null
            getSearchScope(analysisSession, owner)
        } ?: return null

        return analysisSession.chooseCandidateAndCreateSymbol(scope, analysisSession.firSession)
    }

    protected abstract fun KaFirSession.chooseCandidateAndCreateSymbol(candidates: FirScope, firSession: FirSession): S?

    protected open fun getSearchScope(analysisSession: KaFirSession, owner: FirClassSymbol<*>): FirScope? {
        val firSession = analysisSession.firSession
        val scopeSession = analysisSession.getScopeSessionFor(firSession)
        return if (isStatic) {
            val firClass = owner.fir
            firClass.scopeProvider.getStaticCallableMemberScope(firClass, firSession, scopeSession)
        } else {
            owner.unsubstitutedScope(
                useSiteSession = firSession,
                scopeSession = scopeSession,
                withForcedTypeCalculator = false,
                memberRequiredPhase = FirResolvePhase.STATUS,
            )
        }
    }

    abstract override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean
    protected fun hasTheSameOwner(other: KaFirMemberSymbolPointer<*>): Boolean =
        other.isStatic == isStatic && other.ownerPointer.pointsToTheSameSymbolAs(ownerPointer)
}

internal inline fun <reified T : KaSymbol> KaSession.createOwnerPointer(symbol: KaSymbol): KaSymbolPointer<T> {
    val containingSymbol = symbol.containingDeclaration
        ?: errorWithAttachment("Non-null containingDeclaration is expected for a member declaration for `${symbol::class}`, expecting `${T::class}` type of owner") {
            withSymbolAttachment("child", this@createOwnerPointer, symbol)
        }

    requireIsInstance<T>(containingSymbol)

    @Suppress("UNCHECKED_CAST")
    return containingSymbol.createPointer() as KaSymbolPointer<T>
}
