/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.components.KaVisibilityChecker
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirFileSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirPsiJavaClassSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithVisibility
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirSafe
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.collectUseSiteContainers
import org.jetbrains.kotlin.analysis.project.structure.KtDanglingFileModule
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.fir.analysis.checkers.isVisibleInClass
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
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

internal class KaFirVisibilityChecker(
    override val analysisSessionProvider: () -> KaFirSession
) : KaSessionComponent<KaFirSession>(), KaVisibilityChecker, KaFirSessionComponent {
    override fun isVisible(
        candidateSymbol: KaSymbolWithVisibility,
        useSiteFile: KaFileSymbol,
        receiverExpression: KtExpression?,
        position: PsiElement
    ): Boolean = withValidityAssertion {
        require(candidateSymbol is KaFirSymbol<*>)
        require(useSiteFile is KaFirFileSymbol)

        if (candidateSymbol is KaFirPsiJavaClassSymbol) {
            candidateSymbol.isVisibleByPsi(useSiteFile)?.let { return it }
        }

        val dispatchReceiverCanBeExplicit = candidateSymbol is KaCallableSymbol && !candidateSymbol.isExtension
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
     * [isVisibleByPsi] is a heuristic that decides visibility for most [KaFirPsiJavaClassSymbol]s without deferring to its FIR symbol,
     * thereby avoiding lazy construction of the FIR class. The visibility rules are tailored specifically for Java classes accessed from
     * Kotlin. They cover the most popular visibilities `private`, `public`, and default (package) visibility for top-level and nested
     * classes.
     *
     * Returns `null` if visibility cannot be decided by the heuristic.
     */
    private fun KaFirPsiJavaClassSymbol.isVisibleByPsi(useSiteFile: KaFirFileSymbol): Boolean? {
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
                val isSamePackage = classId.packageFqName == useSiteFile.firSymbol.fir.packageFqName
                if (!isSamePackage) return false

                return when (val outerClass = this.outerClass) {
                    null -> true
                    else -> outerClass.isVisibleByPsi(useSiteFile)
                }
            }
        }

        return null
    }

    override fun KaCallableSymbol.isVisibleInClass(classSymbol: KaClassOrObjectSymbol): Boolean = withValidityAssertion {
        require(this is KaFirSymbol<*>)
        require(classSymbol is KaFirSymbol<*>)

        val memberFir = firSymbol.fir as? FirCallableDeclaration ?: return false
        val parentClassFir = classSymbol.firSymbol.fir as? FirClass ?: return false

        // Inspecting visibility requires resolving to status
        classSymbol.firSymbol.lazyResolveToPhase(FirResolvePhase.STATUS)

        return memberFir.symbol.isVisibleInClass(parentClassFir.symbol, memberFir.symbol.resolvedStatus)
    }

    override fun isPublicApi(symbol: KaSymbolWithVisibility): Boolean = withValidityAssertion {
        require(symbol is KaFirSymbol<*>)
        val declaration = symbol.firSymbol.fir as? FirMemberDeclaration ?: return false

        // Inspecting visibility requires resolving to status
        declaration.lazyResolveToPhase(FirResolvePhase.STATUS)
        return declaration.effectiveVisibility.publicApi || declaration.publishedApiEffectiveVisibility?.publicApi == true
    }
}
