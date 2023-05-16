/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.components.KtOverrideInfoProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.buildSymbol
import org.jetbrains.kotlin.analysis.api.fir.components.KtFirAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.getImplementationStatus
import org.jetbrains.kotlin.fir.analysis.checkers.isVisibleInClass
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.originalForIntersectionOverrideAttr
import org.jetbrains.kotlin.fir.originalForSubstitutionOverride
import org.jetbrains.kotlin.fir.resolve.SessionHolderImpl
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.delegatedWrapperData
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.util.ImplementationStatus

internal class KtFirOverrideInfoProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: KtLifetimeToken,
) : KtOverrideInfoProvider(), KtFirAnalysisSessionComponent {

    override fun isVisible(memberSymbol: KtCallableSymbol, classSymbol: KtClassOrObjectSymbol): Boolean {
        require(memberSymbol is KtFirSymbol<*>)
        require(classSymbol is KtFirSymbol<*>)

        val memberFir = memberSymbol.firSymbol.fir as? FirCallableDeclaration ?: return false
        val parentClassFir = classSymbol.firSymbol.fir as? FirClass ?: return false

        // Inspecting visibility requires resolving to status
        classSymbol.firSymbol.lazyResolveToPhase(FirResolvePhase.STATUS)

        return memberFir.isVisibleInClass(parentClassFir)
    }

    override fun getImplementationStatus(memberSymbol: KtCallableSymbol, parentClassSymbol: KtClassOrObjectSymbol): ImplementationStatus? {
        require(memberSymbol is KtFirSymbol<*>)
        require(parentClassSymbol is KtFirSymbol<*>)

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

    override fun getOriginalContainingClassForOverride(symbol: KtCallableSymbol): KtClassOrObjectSymbol? {
        require(symbol is KtFirSymbol<*>)
        val firDeclaration = symbol.firSymbol.fir as FirCallableDeclaration
        val containingClass =
            unwrapFakeOverrides(firDeclaration).containingClassLookupTag()?.toSymbol(rootModuleSession) ?: return null
        return analysisSession.firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(containingClass.fir.symbol) as? KtClassOrObjectSymbol
    }

    override fun unwrapFakeOverrides(symbol: KtCallableSymbol): KtCallableSymbol {
        require(symbol is KtFirSymbol<*>)
        val firDeclaration = symbol.firSymbol.fir as FirCallableDeclaration
        return unwrapFakeOverrides(firDeclaration).buildSymbol(analysisSession.firSymbolBuilder) as KtCallableSymbol
    }

    private fun unwrapFakeOverrides(member: FirCallableDeclaration): FirCallableDeclaration {
        val originalForSubstitutionOverride = member.originalForSubstitutionOverride
        if (originalForSubstitutionOverride != null) return unwrapFakeOverrides(originalForSubstitutionOverride)

        val originalForIntersectionOverrideAttr = member.originalForIntersectionOverrideAttr
        if (originalForIntersectionOverrideAttr != null) return unwrapFakeOverrides(originalForIntersectionOverrideAttr)

        val delegatedWrapperData = member.delegatedWrapperData
        if (delegatedWrapperData != null) return unwrapFakeOverrides(delegatedWrapperData.wrapped)

        return member
    }
}
