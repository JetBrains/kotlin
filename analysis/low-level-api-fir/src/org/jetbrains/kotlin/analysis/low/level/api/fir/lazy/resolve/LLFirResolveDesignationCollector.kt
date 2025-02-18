/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirClassWithAllCallablesResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirWholeElementResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.asResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.tryCollectDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirResolveDesignationCollector.shouldBeResolved
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.isCopyCreatedInScope

/**
 * Collects [LLFirResolveTarget] for requested [FirElementWithResolveState].
 *
 * Effectively, this class is responsible for which elements can be lazily resolved and which cannot.
 *
 * @see LLFirResolveTarget
 * @see shouldBeResolved
 */
object LLFirResolveDesignationCollector {
    fun getDesignationToResolve(target: FirElementWithResolveState): LLFirResolveTarget? {
        return getDesignationToResolve(target, FirDesignation::asResolveTarget)
    }

    fun getDesignationToResolveWithCallableMembers(target: FirRegularClass): LLFirResolveTarget? {
        return getDesignationToResolve(target, ::LLFirClassWithAllCallablesResolveTarget)
    }

    fun getDesignationToResolveRecursively(target: FirElementWithResolveState): LLFirResolveTarget? {
        return getDesignationToResolve(target, ::LLFirWholeElementResolveTarget)
    }

    fun getDesignationToResolve(
        target: FirElementWithResolveState,
        resolveTarget: (FirDesignation) -> LLFirResolveTarget,
    ): LLFirResolveTarget? {
        val designation = getFirDesignationToResolve(target) ?: return null
        val llResolveTarget = resolveTarget(designation)
        return llResolveTarget
    }

    fun getFirDesignationToResolve(target: FirElementWithResolveState): FirDesignation? {
        if (!target.shouldBeResolved()) {
            return null
        }

        return when (target) {
            is FirPropertyAccessor -> getFirDesignationToResolve(target.propertySymbol.fir)
            is FirBackingField -> getFirDesignationToResolve(target.propertySymbol.fir)
            is FirTypeParameter -> getFirDesignationToResolve(target.containingDeclarationSymbol.fir)
            is FirValueParameter -> getFirDesignationToResolve(target.containingDeclarationSymbol.fir)
            is FirReceiverParameter -> getFirDesignationToResolve(target.containingDeclarationSymbol.fir)
            is FirCallableDeclaration if target.isCopyCreatedInScope -> FirDesignation(target)
            else -> target.tryCollectDesignation()
        }
    }

    /**
     * @see isLazyResolvable
     */
    fun FirElementWithResolveState.shouldBeResolved() = when (this) {
        is FirDeclaration -> shouldBeResolved()
        else -> throwUnexpectedFirElementError(this)
    }

    fun FirDeclaration.shouldBeResolved(): Boolean {
        if (!origin.isLazyResolvable) {
            @OptIn(ResolveStateAccess::class)
            check(resolvePhase == FirResolvePhase.BODY_RESOLVE) {
                "Expected body resolve phase for origin $origin but found $resolveState"
            }

            return false
        }

        return true
    }
}
