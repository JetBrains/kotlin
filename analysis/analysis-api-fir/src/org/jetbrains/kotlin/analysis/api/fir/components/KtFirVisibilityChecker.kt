/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.components.KtVisibilityChecker
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirFileSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirPsiJavaClassSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirSafe
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.collectUseSiteContainers
import org.jetbrains.kotlin.analysis.project.structure.KtDanglingFileModule
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.resolve.transformers.publishedApiEffectiveVisibility
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.visibilityChecker
import org.jetbrains.kotlin.psi.*
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

        if (candidateSymbol is KtFirPsiJavaClassSymbol) {
            candidateSymbol.isVisibleByPsi(useSiteFile)?.let { return it }
        }

        val dispatchReceiverCanBeExplicit = candidateSymbol is KtCallableSymbol && !candidateSymbol.isExtension
        val explicitDispatchReceiver = runIf(dispatchReceiverCanBeExplicit) {
            receiverExpression?.getOrBuildFirSafe<FirExpression>(analysisSession.firResolveSession)
        }

        val candidateDeclaration = candidateSymbol.firSymbol.fir as FirMemberDeclaration

        val positionModule = firResolveSession.moduleProvider.getModule(position)
        val candidateModule = candidateDeclaration.llFirModuleData.ktModule

        val effectiveSession = if (positionModule is KtDanglingFileModule && candidateModule != positionModule) {
            @Suppress("USELESS_CAST") // Smart cast is only available in K2
            val contextModule = (positionModule as KtDanglingFileModule).contextModule
            firResolveSession.getSessionFor(contextModule)
        } else {
            firResolveSession.getSessionFor(positionModule)
        }

        val effectiveContainers = collectUseSiteContainers(position, firResolveSession).orEmpty()

        return effectiveSession.visibilityChecker.isVisible(
            candidateDeclaration,
            effectiveSession,
            useSiteFile.firSymbol.fir,
            effectiveContainers,
            explicitDispatchReceiver
        )
    }

    /**
     * [isVisibleByPsi] is a heuristic that decides visibility for most [KtFirPsiJavaClassSymbol]s without deferring to its FIR symbol,
     * thereby avoiding lazy construction of the FIR class. The visibility rules are tailored specifically for Java classes accessed from
     * Kotlin. They cover the most popular visibilities `private`, `public`, and default (package) visibility for top-level and nested
     * classes.
     *
     * Returns `null` if visibility cannot be decided by the heuristic.
     */
    private fun KtFirPsiJavaClassSymbol.isVisibleByPsi(useSiteFile: KtFirFileSymbol): Boolean? {
        when (visibility) {
            Visibilities.Private ->
                // Private classes from Java cannot be accessed from Kotlin.
                return false

            Visibilities.Public ->
                return when (val outerClass = this.outerClass) {
                    null -> true
                    else -> outerClass.isVisibleByPsi(useSiteFile)
                }

            JavaVisibilities.PackageVisibility -> {
                val isSamePackage = classIdIfNonLocal.packageFqName == useSiteFile.firSymbol.fir.packageFqName
                if (!isSamePackage) return false

                return when (val outerClass = this.outerClass) {
                    null -> true
                    else -> outerClass.isVisibleByPsi(useSiteFile)
                }
            }
        }

        return null
    }

    override fun isPublicApi(symbol: KtSymbolWithVisibility): Boolean {
        require(symbol is KtFirSymbol<*>)
        val declaration = symbol.firSymbol.fir as? FirMemberDeclaration ?: return false

        // Inspecting visibility requires resolving to status
        declaration.lazyResolveToPhase(FirResolvePhase.STATUS)
        return declaration.effectiveVisibility.publicApi || declaration.publishedApiEffectiveVisibility?.publicApi == true
    }
}
