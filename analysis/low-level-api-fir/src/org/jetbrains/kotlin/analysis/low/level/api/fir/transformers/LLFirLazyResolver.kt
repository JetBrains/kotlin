/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector

internal abstract class LLFirLazyResolver(
    val resolverPhase: FirResolvePhase,
) {
    abstract fun resolve(
        target: LLFirResolveTarget,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirTowerDataContextCollector?,
    )

    abstract fun checkIsResolved(target: FirElementWithResolveState)

    abstract fun updatePhaseForDeclarationInternals(target: FirElementWithResolveState)

    fun checkIsResolved(designation: LLFirResolveTarget) {
        designation.forEachTarget(::checkIsResolved)
    }

    protected fun checkNestedDeclarationsAreResolved(target: FirElementWithResolveState) {
        if (target !is FirDeclaration) return
        checkFunctionParametersAreResolved(target)
        checkPropertyAccessorsAreResolved(target)
        checkPropertyBackingFieldIsResolved(target)
        checkTypeParametersAreResolved(target)
    }

    private fun checkPropertyAccessorsAreResolved(declaration: FirDeclaration) {
        if (declaration is FirProperty) {
            declaration.getter?.let { checkIsResolved(it) }
            declaration.setter?.let { checkIsResolved(it) }
        }
    }


    private fun checkPropertyBackingFieldIsResolved(declaration: FirDeclaration) {
        if (declaration is FirProperty) {
            declaration.backingField?.let { checkIsResolved(it) }
        }
    }


    private fun checkFunctionParametersAreResolved(declaration: FirDeclaration) {
        if (declaration is FirFunction) {
            for (parameter in declaration.valueParameters) {
                checkIsResolved(parameter)
            }
        }
    }

    private fun checkTypeParametersAreResolved(declaration: FirDeclaration) {
        if (declaration is FirTypeParameterRefsOwner) {
            for (parameter in declaration.typeParameters) {
                if (parameter is FirTypeParameter) {
                    checkIsResolved(parameter)
                }
            }
        }
    }
}
