/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.resolve.calls.ExpressionReceiverValue
import org.jetbrains.kotlin.fir.visibilityChecker
import org.jetbrains.kotlin.idea.fir.low.level.api.api.collectDesignation
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirSafe
import org.jetbrains.kotlin.idea.fir.low.level.api.util.parentsOfType
import org.jetbrains.kotlin.idea.frontend.api.components.KtVisibilityChecker
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirFileSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

internal class KtFirVisibilityChecker(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken
) : KtVisibilityChecker(), KtFirAnalysisSessionComponent {

    override fun isVisible(
        candidateSymbol: KtSymbolWithVisibility,
        useSiteFile: KtFileSymbol,
        position: PsiElement,
        receiverExpression: KtExpression?
    ): Boolean {
        require(candidateSymbol is KtFirSymbol<*>)
        require(useSiteFile is KtFirFileSymbol)

        val nonLocalContainingDeclaration = findContainingNonLocalDeclaration(position)

        return useSiteFile.firRef.withFir { useSiteFirFile ->
            val containers = nonLocalContainingDeclaration
                ?.getOrBuildFirSafe<FirCallableDeclaration<*>>(analysisSession.firResolveState)
                ?.collectDesignation()
                ?.path
                .orEmpty()

            val explicitDispatchReceiver = receiverExpression
                ?.getOrBuildFirSafe<FirExpression>(analysisSession.firResolveState)
                ?.let { ExpressionReceiverValue(it) }

            candidateSymbol.firRef.withFir { candidateFirSymbol ->
                require(candidateFirSymbol is FirMemberDeclaration && candidateFirSymbol is FirSymbolOwner<*>) {
                    "$candidateFirSymbol must be a FirMemberDeclaration and FirSymbolOwner; it were ${candidateFirSymbol::class} instead"
                }

                rootModuleSession.visibilityChecker.isVisible(
                    candidateFirSymbol,
                    rootModuleSession,
                    useSiteFirFile,
                    containers,
                    explicitDispatchReceiver
                )
            }
        }
    }

    private fun findContainingNonLocalDeclaration(element: PsiElement): KtCallableDeclaration? {
        return element
            .parentsOfType<KtCallableDeclaration>()
            .firstOrNull { it.isNotFromLocalClass }
    }

    private val KtCallableDeclaration.isNotFromLocalClass
        get() = this is KtNamedFunction && (isTopLevel || containingClassOrObject?.isLocal == false) ||
                this is KtProperty && (isTopLevel || containingClassOrObject?.isLocal == false)
}
