/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirPhaseRunner
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDeclarationDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.ResolveTreeBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirLazyTransformer.Companion.updatePhaseDeep
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.*
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirTypeResolveTransformer

/**
 * Transform designation into TYPES phase. Affects only for designation, target declaration and it's children
 */
internal class LLFirDesignatedTypeResolverTransformer(
    private val designation: FirDeclarationDesignationWithFile,
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

    override fun transformDeclaration(phaseRunner: LLFirPhaseRunner) {
        if (designation.declaration.resolvePhase >= FirResolvePhase.TYPES) return
        designation.declaration.checkPhase(FirResolvePhase.SUPER_TYPES)

        ResolveTreeBuilder.resolvePhase(designation.declaration, FirResolvePhase.TYPES) {
            phaseRunner.runPhaseWithCustomResolve(FirResolvePhase.TYPES) {
                designation.firFile.transform<FirFile, Any?>(this, null)
            }
        }

        declarationTransformer.ensureDesignationPassed()
        updatePhaseDeep(designation.declaration, FirResolvePhase.TYPES)

        checkIsResolved(designation.declaration)
        checkClassMembersAreResolved(designation.declaration)
    }

    override fun checkIsResolved(declaration: FirDeclaration) {
        declaration.checkPhase(FirResolvePhase.TYPES)
        when (declaration) {
            is FirCallableDeclaration -> {
                checkReturnTypeRefIsResolved(declaration, acceptImplicitTypeRef = true)
                checkReceiverTypeRefIsResolved(declaration)
            }

            is FirTypeParameter -> {
                for (bound in declaration.bounds) {
                    checkTypeRefIsResolved(bound, "type parameter bound", declaration)
                }
            }

            else -> {}
        }
        checkNestedDeclarationsAreResolved(declaration)
        checkTypeParametersAreResolved(declaration)
    }
}
