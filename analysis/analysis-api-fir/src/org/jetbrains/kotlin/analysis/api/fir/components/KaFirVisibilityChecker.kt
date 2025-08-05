/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.analysis.api.impl.base.components.withPsiValidityAssertion
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirSafe
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.collectUseSiteContainers
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.analysis.utils.printer.parentsOfType
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.isVisibleInClass
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.transformers.publishedApiEffectiveVisibility
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.visibilityChecker
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression

internal class KaFirVisibilityChecker(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseSessionComponent<KaFirSession>(), KaVisibilityChecker, KaFirSessionComponent {
    override fun createUseSiteVisibilityChecker(
        useSiteFile: KaFileSymbol,
        receiverExpression: KtExpression?,
        position: PsiElement,
    ): KaUseSiteVisibilityChecker = withPsiValidityAssertion(receiverExpression, position) {
        require(useSiteFile is KaFirFileSymbol)

        val dispatchReceiver = receiverExpression?.getOrBuildFirSafe<FirExpression>(analysisSession.resolutionFacade)
        val positionModule = resolutionFacade.moduleProvider.getModule(position)
        val containingDeclarations = collectContainingDeclarations(position)

        KaFirUseSiteVisibilityChecker(
            position,
            positionModule,
            containingDeclarations,
            dispatchReceiver,
            useSiteFile,
            analysisSession,
            token,
        )
    }

    private fun collectContainingDeclarations(position: PsiElement): List<FirDeclaration> {
        val nonLocalLazilyResolvedContainers = collectUseSiteContainers(position, resolutionFacade).orEmpty()
        val closestNonLocalElement = nonLocalLazilyResolvedContainers.lastOrNull()?.psi ?: return nonLocalLazilyResolvedContainers
        val localFullyResolvedContainers = position.parentsOfType<KtClassOrObject>()
            .takeWhile { it != closestNonLocalElement }
            .map { it.resolveToFirSymbol(resolutionFacade).fir }
            .toList()

        return nonLocalLazilyResolvedContainers + localFullyResolvedContainers
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
    private val position: PsiElement,
    private val positionModule: KaModule,
    private val containingDeclarations: List<FirDeclaration>,
    private val dispatchReceiver: FirExpression?,
    private val useSiteFile: KaFirFileSymbol,
    private val analysisSession: KaFirSession,
    override val token: KaLifetimeToken,
) : KaUseSiteVisibilityChecker {
    override fun isVisible(candidateSymbol: KaDeclarationSymbol): Boolean = withValidityAssertion {
        require(candidateSymbol is KaFirSymbol<*>)

        if (candidateSymbol is KaFirPsiJavaClassSymbol) {
            candidateSymbol.isVisibleByPsi(useSiteFile)?.let { return it }
        }

        val candidateDeclaration = candidateSymbol.firSymbol.fir as? FirMemberDeclaration ?: return true
        val dispatchReceiverCanBeExplicit = candidateSymbol is KaCallableSymbol && !candidateSymbol.isExtension
        val explicitDispatchReceiver = dispatchReceiver.takeIf { dispatchReceiverCanBeExplicit }
        val targetSession = getTargetSession(candidateDeclaration)

        if (targetSession.visibilityChecker.isVisible(
                candidateDeclaration,
                targetSession,
                useSiteFile.firSymbol.fir,
                containingDeclarations,
                explicitDispatchReceiver
            )
        ) {
            return true
        }

        return isVisibleFromSuperInterfaceOfImplicitReceiver(candidateSymbol, explicitDispatchReceiver)
    }

    private fun getTargetSession(candidateDeclaration: FirMemberDeclaration): LLFirSession {
        val candidateModule = candidateDeclaration.llFirModuleData.ktModule
        val targetModule = if (positionModule is KaDanglingFileModule && candidateModule != positionModule) {
            positionModule.contextModule
        } else {
            positionModule
        }
        return analysisSession.resolutionFacade.getSessionFor(targetModule)
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

    // Handle a special case: public members of "exposed" non-public super interfaces from implicit receivers (see KT-78597)
    private fun isVisibleFromSuperInterfaceOfImplicitReceiver(
        candidateSymbol: KaDeclarationSymbol,
        explicitDispatchReceiver: FirExpression?,
    ): Boolean {
        if (explicitDispatchReceiver != null) return false

        if (candidateSymbol !is KaCallableSymbol) return false
        if (candidateSymbol.visibility != KaSymbolVisibility.PUBLIC) {
            // Interface members can't have internal or protected visibility, and private members are definitely not visible
            return false
        }

        with(analysisSession) {
            val containingSymbol = candidateSymbol.containingSymbol as? KaClassSymbol ?: return false
            if (containingSymbol.classKind != KaClassKind.INTERFACE) return false

            // `position` may be a leaf PsiElement
            val ktPosition = position.parentOfType<KtElement>(withSelf = true) ?: return false

            val scopeContext = ktPosition.containingKtFile.scopeContext(ktPosition)
            for (implicitReceiver in scopeContext.implicitReceivers) {
                val classSymbol = implicitReceiver.type.symbol as? KaClassSymbol ?: continue
                if (classSymbol.isSubClassOf(containingSymbol)) {
                    return true
                }
            }

            return false
        }
    }
}