/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import org.jetbrains.kotlin.fir.analysis.checkers.getImplementationStatus
import org.jetbrains.kotlin.fir.analysis.checkers.isVisibleInClass
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.originalForIntersectionOverrideAttr
import org.jetbrains.kotlin.fir.originalForSubstitutionOverride
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.SessionHolderImpl
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.impl.delegatedWrapperData
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.components.KtOverrideInfoProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.buildSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.components.KtFirAnalysisSessionComponent
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.util.ImplementationStatus

internal class KtFirOverrideInfoProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtOverrideInfoProvider(), KtFirAnalysisSessionComponent {

    override fun isVisible(memberSymbol: KtCallableSymbol, classSymbol: KtClassOrObjectSymbol): Boolean {
        require(memberSymbol is KtFirSymbol<*>)
        require(classSymbol is KtFirSymbol<*>)

        // Inspecting visibility requires resolving to status
        return memberSymbol.firRef.withFir(FirResolvePhase.STATUS) outer@{ memberFir ->
            if (memberFir !is FirCallableDeclaration) return@outer false

            classSymbol.firRef.withFir inner@{ parentClassFir ->
                if (parentClassFir !is FirClass) return@inner false

                memberFir.isVisibleInClass(parentClassFir)
            }
        }
    }

    override fun getImplementationStatus(memberSymbol: KtCallableSymbol, parentClassSymbol: KtClassOrObjectSymbol): ImplementationStatus? {
        require(memberSymbol is KtFirSymbol<*>)
        require(parentClassSymbol is KtFirSymbol<*>)

        // Inspecting implementation status requires resolving to status
        return memberSymbol.firRef.withFir(FirResolvePhase.STATUS) outer@{ memberFir ->
            if (memberFir !is FirCallableDeclaration) return@outer null

            parentClassSymbol.firRef.withFir inner@{ parentClassFir ->
                if (parentClassFir !is FirClass) return@inner null

                memberFir.symbol.getImplementationStatus(
                    SessionHolderImpl(rootModuleSession, ScopeSession()),
                    parentClassFir.symbol
                )
            }
        }
    }

    override fun getOriginalContainingClassForOverride(symbol: KtCallableSymbol): KtClassOrObjectSymbol? {
        require(symbol is KtFirSymbol<*>)
        return symbol.firRef.withFir(FirResolvePhase.STATUS) { firDeclaration ->
            if (firDeclaration !is FirCallableDeclaration) return@withFir null
            val containingClass =
                getOriginalOverriddenSymbol(firDeclaration)?.containingClass()?.toSymbol(rootModuleSession) ?: return@withFir null
            analysisSession.firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(containingClass.fir) as? KtClassOrObjectSymbol
        }
    }

    override fun getOriginalOverriddenSymbol(symbol: KtCallableSymbol): KtCallableSymbol? {
        require(symbol is KtFirSymbol<*>)
        return symbol.firRef.withFir(FirResolvePhase.STATUS) { firDeclaration ->
            if (firDeclaration !is FirCallableDeclaration) return@withFir null
            with(analysisSession) {
                getOriginalOverriddenSymbol(firDeclaration)
                    ?.buildSymbol((analysisSession as KtFirAnalysisSession).firSymbolBuilder) as KtCallableSymbol?
            }
        }
    }

    private fun getOriginalOverriddenSymbol(member: FirCallableDeclaration): FirCallableDeclaration? {
        val originalForSubstitutionOverride = member.originalForSubstitutionOverride
        if (originalForSubstitutionOverride != null) return getOriginalOverriddenSymbol(originalForSubstitutionOverride)

        val originalForIntersectionOverrideAttr = member.originalForIntersectionOverrideAttr
        if (originalForIntersectionOverrideAttr != null) return getOriginalOverriddenSymbol(originalForIntersectionOverrideAttr)

        val delegatedWrapperData = member.delegatedWrapperData
        if (delegatedWrapperData != null) return getOriginalOverriddenSymbol(delegatedWrapperData.wrapped)

        return member
    }
}