/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.components.KaUseSiteVisibilityChecker
import org.jetbrains.kotlin.analysis.api.components.KaVisibilityChecker
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirFileSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirPsiJavaClassSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolVisibility
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirSafe
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.collectUseSiteContainers
import org.jetbrains.kotlin.fir.analysis.checkers.isVisibleInClass
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
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
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseSessionComponent<KaFirSession>(), KaVisibilityChecker, KaFirSessionComponent {
    override fun createUseSiteVisibilityChecker(
        useSiteFile: KaFileSymbol,
        receiverExpression: KtExpression?,
        position: PsiElement,
    ): KaUseSiteVisibilityChecker = withValidityAssertion {
        require(useSiteFile is KaFirFileSymbol)

        val dispatchReceiver = receiverExpression?.getOrBuildFirSafe<FirExpression>(analysisSession.resolutionFacade)

        val positionModule = resolutionFacade.moduleProvider.getModule(position)
        val effectiveContainers = collectUseSiteContainers(position, resolutionFacade).orEmpty()

        KaFirUseSiteVisibilityChecker(
            positionModule,
            effectiveContainers,
            dispatchReceiver,
            useSiteFile,
            resolutionFacade,
            token,
        )
    }

    override fun KaCallableSymbol.isVisibleInClass(classSymbol: KaClassSymbol): Boolean = withValidityAssertion {
        if (this is KaReceiverParameterSymbol) {
            // Receiver parameters are local
            return false
        }

        require(this is KaFirSymbol<*>)
        require(classSymbol is KaFirSymbol<*>)

        val memberFir = firSymbol.fir as? FirCallableDeclaration ?: return false
        val parentClassFir = classSymbol.firSymbol.fir as? FirClass ?: return false

        // Inspecting visibility requires resolving to status
        classSymbol.firSymbol.lazyResolveToPhase(FirResolvePhase.STATUS)

        return memberFir.symbol.isVisibleInClass(parentClassFir.symbol, memberFir.symbol.resolvedStatus)
    }

    override fun isPublicApi(symbol: KaDeclarationSymbol): Boolean = withValidityAssertion {
        if (symbol is KaReceiverParameterSymbol) {
            return isPublicApi(symbol.owningCallableSymbol)
        }

        require(symbol is KaFirSymbol<*>)
        val declaration = symbol.firSymbol.fir as? FirMemberDeclaration ?: return false

        // Inspecting visibility requires resolving to status
        declaration.lazyResolveToPhase(FirResolvePhase.STATUS)
        return declaration.effectiveVisibility.publicApi || declaration.publishedApiEffectiveVisibility?.publicApi == true
    }
}


private class KaFirUseSiteVisibilityChecker(
    private val positionModule: KaModule,
    private val effectiveContainers: List<FirDeclaration>,
    private val dispatchReceiver: FirExpression?,
    private val useSiteFile: KaFirFileSymbol,
    private val resolutionFacade: LLResolutionFacade,
    override val token: KaLifetimeToken,
) : KaUseSiteVisibilityChecker {
    override fun isVisible(candidateSymbol: KaDeclarationSymbol): Boolean = withValidityAssertion {
        require(candidateSymbol is KaFirSymbol<*>)

        if (candidateSymbol is KaFirPsiJavaClassSymbol) {
            candidateSymbol.isVisibleByPsi(useSiteFile)?.let { return it }
        }

        val candidateDeclaration = candidateSymbol.firSymbol.fir as? FirMemberDeclaration ?: return true

        val dispatchReceiverCanBeExplicit = candidateSymbol is KaCallableSymbol && !candidateSymbol.isExtension
        val explicitDispatchReceiver = runIf(dispatchReceiverCanBeExplicit) { dispatchReceiver }

        val candidateModule = candidateDeclaration.llFirModuleData.ktModule

        val effectiveSession = if (positionModule is KaDanglingFileModule && candidateModule != positionModule) {
            @Suppress("USELESS_CAST") // Smart cast is only available in K2
            val contextModule = (positionModule as KaDanglingFileModule).contextModule
            resolutionFacade.getSessionFor(contextModule)
        } else {
            resolutionFacade.getSessionFor(positionModule)
        }

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
    private fun KaFirPsiJavaClassSymbol.isVisibleByPsi(useSiteFile: KaFirFileSymbol): Boolean? = when (visibility) {
        KaSymbolVisibility.PRIVATE ->
            // Private classes from Java cannot be accessed from Kotlin.
            false

        KaSymbolVisibility.PUBLIC -> when (val outerClass = this.outerClass) {
            null -> true
            else -> outerClass.isVisibleByPsi(useSiteFile)
        }

        KaSymbolVisibility.PACKAGE_PRIVATE -> {
            val isSamePackage = classId.packageFqName == useSiteFile.firSymbol.fir.packageFqName
            if (!isSamePackage) false
            else when (val outerClass = this.outerClass) {
                null -> true
                else -> outerClass.isVisibleByPsi(useSiteFile)
            }
        }

        else -> null
    }
}