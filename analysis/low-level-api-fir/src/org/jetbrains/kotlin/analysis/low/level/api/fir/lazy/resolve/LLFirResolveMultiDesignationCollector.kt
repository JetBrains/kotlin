/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirClassWithAllCallablesResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirSingleResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirWholeElementResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.asResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.tryCollectDesignationWithFile
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticPropertyAccessor
import org.jetbrains.kotlin.fir.isCopyCreatedInScope

internal object LLFirResolveMultiDesignationCollector {
    fun getDesignationsToResolve(target: FirElementWithResolveState): List<LLFirResolveTarget> = when (target) {
        is FirFile -> listOf(FirDesignation(target).asResolveTarget())
        is FirSyntheticPropertyAccessor -> getDesignationsToResolve(target.delegate)
        is FirSyntheticProperty -> getDesignationsToResolve(target.getter) + target.setter?.let(::getDesignationsToResolve).orEmpty()
        else -> listOfNotNull(getMainDesignationToResolve(target))
    }

    fun getDesignationsToResolveWithCallableMembers(target: FirRegularClass): List<LLFirResolveTarget> {
        val designation = target.tryCollectDesignationWithFile() ?: return emptyList()
        val resolveTarget = LLFirClassWithAllCallablesResolveTarget(designation)
        return listOf(resolveTarget)
    }

    fun getDesignationsToResolveRecursively(target: FirElementWithResolveState): List<LLFirResolveTarget> {
        if (target is FirFile) return listOf(LLFirWholeElementResolveTarget(FirDesignation(target)))

        if (!target.shouldBeResolved()) return emptyList()
        if (target is FirCallableDeclaration && target.isCopyCreatedInScope) {
            return listOf(FirDesignation(target).asResolveTarget())
        }

        val designation = target.tryCollectDesignationWithFile() ?: return emptyList()
        val resolveTarget = LLFirWholeElementResolveTarget(designation)
        return listOf(resolveTarget)
    }

    private fun getMainDesignationToResolve(target: FirElementWithResolveState): LLFirSingleResolveTarget? {
        require(target !is FirFile)
        if (!target.shouldBeResolved()) return null
        return when {
            target is FirPropertyAccessor -> getMainDesignationToResolve(target.propertySymbol.fir)
            target is FirBackingField -> getMainDesignationToResolve(target.propertySymbol.fir)
            target is FirTypeParameter -> getMainDesignationToResolve(target.containingDeclarationSymbol.fir)
            target is FirValueParameter -> getMainDesignationToResolve(target.containingFunctionSymbol.fir)
            target is FirCallableDeclaration && target.isCopyCreatedInScope -> FirDesignation(target).asResolveTarget()
            else -> target.tryCollectDesignationWithFile()?.asResolveTarget()
        }
    }

    private fun FirElementWithResolveState.shouldBeResolved() = when (this) {
        is FirDeclaration -> shouldBeResolved()
        is FirFileAnnotationsContainer -> annotations.isNotEmpty()
        else -> throwUnexpectedFirElementError(this)
    }

    private fun FirDeclaration.shouldBeResolved(): Boolean {
        if (!origin.isLazyResolvable) {
            @OptIn(ResolveStateAccess::class)
            check(resolvePhase == FirResolvePhase.BODY_RESOLVE) {
                "Expected body resolve phase for origin $origin but found $resolveState"
            }

            return false
        }

        return when (this) {
            is FirFile -> true
            is FirSyntheticProperty, is FirSyntheticPropertyAccessor -> false
            is FirSimpleFunction,
            is FirProperty,
            is FirPropertyAccessor,
            is FirField,
            is FirTypeAlias,
            is FirConstructor,
            -> true
            else -> true
        }
    }
}
