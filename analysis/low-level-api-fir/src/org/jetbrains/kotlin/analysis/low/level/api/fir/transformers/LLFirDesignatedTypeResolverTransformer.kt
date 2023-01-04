/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyBodiesCalculator
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirPhaseUpdater
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkReceiverTypeRefIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkReturnTypeRefIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkTypeRefIsResolved
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirTypeResolveTransformer
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef

/**
 * Transform designation into TYPES phase. Affects only for designation, target declaration and it's children
 */
internal class LLFirDesignatedTypeResolverTransformer(
    private val designation: FirDesignationWithFile,
    session: FirSession,
    scopeSession: ScopeSession,
) : LLFirLazyTransformer, FirTypeResolveTransformer(session, scopeSession) {

    private val declarationTransformer = LLFirDeclarationTransformer(designation)

    override fun <E : FirElement> transformElement(element: E, data: Any?): E {
        return if (element is FirDeclaration && (element is FirRegularClass || element is FirFile)) {
            @Suppress("UNCHECKED_CAST")
            declarationTransformer.transformDeclarationContent(this, element, data) {
                super.transformElement(element, data)
            } as E
        } else {
            super.transformElement(element, data)
        }
    }

    override fun transformDeclaration() {
        designation.firFile.transform<FirFile, Any?>(this, null)
        declarationTransformer.ensureDesignationPassed()
    }

    override fun updatePhaseForDeclarationInternals(target: FirElementWithResolveState) {
        LLFirPhaseUpdater.updateDeclarationInternalsPhase(target, FirResolvePhase.TYPES, updateForLocalDeclarations = false)
    }

    override fun transformTypeRef(typeRef: FirTypeRef, data: Any?): FirResolvedTypeRef {
        FirLazyBodiesCalculator.calculateAnnotations(typeRef, session)
        return super.transformTypeRef(typeRef, data)
    }

    override fun checkIsResolved(target: FirElementWithResolveState) {
        target.checkPhase(FirResolvePhase.TYPES)
        when (target) {
            is FirCallableDeclaration -> {
                checkReturnTypeRefIsResolved(target, acceptImplicitTypeRef = true)
                checkReceiverTypeRefIsResolved(target)
            }

            is FirTypeParameter -> {
                for (bound in target.bounds) {
                    checkTypeRefIsResolved(bound, "type parameter bound", target)
                }
            }

            else -> {}
        }
        checkNestedDeclarationsAreResolved(target)
        (target as? FirDeclaration)?.let {
            checkTypeParametersAreResolved(it)
            checkClassMembersAreResolved(it)
        }
    }
}
