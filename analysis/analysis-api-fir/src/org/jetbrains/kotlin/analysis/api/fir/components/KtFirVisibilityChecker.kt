/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.components.KtVisibilityChecker
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirFileSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirSafe
import org.jetbrains.kotlin.analysis.utils.printer.parentsOfType
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.resolve.calls.ExpressionReceiverValue
import org.jetbrains.kotlin.fir.resolve.transformers.publishedApiEffectiveVisibility
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.visibilityChecker
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.utils.addToStdlib.runIf

internal class KtFirVisibilityChecker(
    override val analysisSession: KtFirAnalysisSession,
    override val token: KtLifetimeToken
) : KtVisibilityChecker(), KtFirAnalysisSessionComponent {

    override fun isVisible(
        candidateSymbol: KtSymbolWithVisibility,
        useSiteFile: KtFileSymbol,
        position: PsiElement,
        receiverExpression: KtExpression?
    ): Boolean {
        require(candidateSymbol is KtFirSymbol<*>)
        require(useSiteFile is KtFirFileSymbol)

        val useSiteFirFile = useSiteFile.firSymbol.fir
        val containers = collectContainingDeclarations(position)

        val dispatchReceiverCanBeExplicit = candidateSymbol is KtCallableSymbol && !candidateSymbol.isExtension
        val explicitDispatchReceiver = runIf(dispatchReceiverCanBeExplicit) {
            receiverExpression
                ?.getOrBuildFirSafe<FirExpression>(analysisSession.firResolveSession)
                ?.let { ExpressionReceiverValue(it) }
        }

        val candidateFirSymbol = candidateSymbol.firSymbol.fir as FirMemberDeclaration

        return rootModuleSession.visibilityChecker.isVisible(
            candidateFirSymbol,
            rootModuleSession,
            useSiteFirFile,
            containers,
            explicitDispatchReceiver
        )
    }

    override fun isPublicApi(symbol: KtSymbolWithVisibility): Boolean {
        require(symbol is KtFirSymbol<*>)
        val declaration = symbol.firSymbol.fir as? FirMemberDeclaration ?: return false

        // Inspecting visibility requires resolving to status
        declaration.lazyResolveToPhase(FirResolvePhase.STATUS)
        return declaration.effectiveVisibility.publicApi || declaration.publishedApiEffectiveVisibility?.publicApi == true
    }

    private fun collectContainingDeclarations(position: PsiElement): List<FirDeclaration> {
        val nonLocalContainer = findContainingNonLocalDeclaration(position)
        val nonLocalContainerFir = nonLocalContainer?.getOrBuildFirSafe<FirDeclaration>(analysisSession.firResolveSession)
            ?: return emptyList()

        val designation = nonLocalContainerFir.collectDesignation()

        return designation
            .toSequence(includeTarget = true) // we include the starting declaration in case it is a class or an object
            .toList()
    }

    private fun findContainingNonLocalDeclaration(element: PsiElement): KtDeclaration? {
        return element
            .parentsOfType<KtDeclaration>()
            .firstOrNull { it.isNotLocal }
    }

    private val KtDeclaration.isNotLocal
        get() = this is KtNamedFunction && (isTopLevel || containingClassOrObject?.isLocal == false) ||
                this is KtProperty && (isTopLevel || containingClassOrObject?.isLocal == false) ||
                this is KtClassOrObject && (isTopLevel() || !isLocal)
}
