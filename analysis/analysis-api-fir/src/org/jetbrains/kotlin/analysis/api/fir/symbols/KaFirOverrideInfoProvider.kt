/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.components.KaOverrideInfoProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.buildSymbol
import org.jetbrains.kotlin.analysis.api.fir.components.KaFirSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.getImplementationStatus
import org.jetbrains.kotlin.fir.analysis.checkers.isVisibleInClass
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.SessionHolderImpl
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.util.ImplementationStatus

internal class KaFirOverrideInfoProvider(
    override val analysisSession: KaFirSession,
    override val token: KaLifetimeToken,
) : KaOverrideInfoProvider(), KaFirSessionComponent {

    override fun isVisible(memberSymbol: KaCallableSymbol, classSymbol: KaClassOrObjectSymbol): Boolean {
        require(memberSymbol is KaFirSymbol<*>)
        require(classSymbol is KaFirSymbol<*>)

        val memberFir = memberSymbol.firSymbol.fir as? FirCallableDeclaration ?: return false
        val parentClassFir = classSymbol.firSymbol.fir as? FirClass ?: return false

        // Inspecting visibility requires resolving to status
        classSymbol.firSymbol.lazyResolveToPhase(FirResolvePhase.STATUS)

        return memberFir.isVisibleInClass(parentClassFir)
    }

    override fun getImplementationStatus(memberSymbol: KaCallableSymbol, parentClassSymbol: KaClassOrObjectSymbol): ImplementationStatus? {
        require(memberSymbol is KaFirSymbol<*>)
        require(parentClassSymbol is KaFirSymbol<*>)

        // Inspecting implementation status requires resolving to status
        val memberFir = memberSymbol.firSymbol.fir as? FirCallableDeclaration ?: return null
        val parentClassFir = parentClassSymbol.firSymbol.fir as? FirClass ?: return null
        memberFir.lazyResolveToPhase(FirResolvePhase.STATUS)

        return memberFir.symbol.getImplementationStatus(
            SessionHolderImpl(
                rootModuleSession,
                analysisSession.getScopeSessionFor(analysisSession.useSiteSession),
            ),
            parentClassFir.symbol
        )
    }

    override fun getOriginalContainingClassForOverride(symbol: KaCallableSymbol): KaClassOrObjectSymbol? {
        require(symbol is KaFirSymbol<*>)

        val targetDeclaration = symbol.firSymbol.fir as FirCallableDeclaration
        val unwrappedDeclaration = targetDeclaration.unwrapFakeOverridesOrDelegated()

        val unwrappedFirSymbol = unwrappedDeclaration.symbol
        val unwrappedKtSymbol = analysisSession.firSymbolBuilder.callableBuilder.buildCallableSymbol(unwrappedFirSymbol)
        return with(analysisSession) { unwrappedKtSymbol.getContainingSymbol() as? KaClassOrObjectSymbol }
    }

    override fun unwrapFakeOverrides(symbol: KaCallableSymbol): KaCallableSymbol {
        require(symbol is KaFirSymbol<*>)

        val originalDeclaration = symbol.firSymbol.fir as FirCallableDeclaration
        val unwrappedDeclaration = originalDeclaration.unwrapFakeOverridesOrDelegated()

        return unwrappedDeclaration.buildSymbol(analysisSession.firSymbolBuilder) as KaCallableSymbol
    }
}
