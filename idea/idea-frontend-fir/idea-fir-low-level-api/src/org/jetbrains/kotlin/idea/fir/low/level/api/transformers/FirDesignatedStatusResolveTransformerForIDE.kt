/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirStatusResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.StatusComputationSession
import org.jetbrains.kotlin.idea.fir.low.level.api.FirPhaseRunner
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationDesignationWithFile
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.FirLazyTransformerForIDE.Companion.updatePhaseDeep
import org.jetbrains.kotlin.idea.fir.low.level.api.util.ensurePhase

/**
 * Transform designation into STATUS phase. Affects only for designation, target declaration, it's children and dependents
 */
internal class FirDesignatedStatusResolveTransformerForIDE(
    private val designation: FirDeclarationDesignationWithFile,
    private val session: FirSession,
    private val scopeSession: ScopeSession,
) : FirLazyTransformerForIDE {
    private inner class FirDesignatedStatusResolveTransformerForIDE :
        FirStatusResolveTransformer(session, scopeSession, StatusComputationSession.Regular()) {

        val designationTransformer = IDEDeclarationTransformer(designation)

        override fun transformDeclarationContent(declaration: FirDeclaration, data: FirResolvedDeclarationStatus?): FirDeclaration =
            designationTransformer.transformDeclarationContent(this, declaration, data) {
                super.transformDeclarationContent(declaration, data)
            }
    }

    override fun transformDeclaration(phaseRunner: FirPhaseRunner) {
        if (designation.declaration.resolvePhase >= FirResolvePhase.STATUS) return
        designation.declaration.ensurePhase(FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS)

        val transformer = FirDesignatedStatusResolveTransformerForIDE()
        phaseRunner.runPhaseWithCustomResolve(FirResolvePhase.STATUS) {
            designation.firFile.transform<FirElement, FirResolvedDeclarationStatus?>(transformer, null)
        }

        transformer.designationTransformer.ensureDesignationPassed()
        updatePhaseDeep(designation.declaration, FirResolvePhase.STATUS)
        ensureResolved(designation.declaration)
        ensureResolvedDeep(designation.declaration)
    }

    override fun ensureResolved(declaration: FirDeclaration) {
        if (declaration !is FirAnonymousInitializer) {
            declaration.ensurePhase(FirResolvePhase.STATUS)
        }
        when (declaration) {
            is FirSimpleFunction -> check(declaration.status is FirResolvedDeclarationStatus)
            is FirConstructor -> check(declaration.status is FirResolvedDeclarationStatus)
            is FirTypeAlias -> check(declaration.status is FirResolvedDeclarationStatus)
            is FirEnumEntry -> check(declaration.status is FirResolvedDeclarationStatus)
            is FirField -> check(declaration.status is FirResolvedDeclarationStatus)
            is FirProperty -> {
                check(declaration.status is FirResolvedDeclarationStatus)
                check(declaration.getter?.status?.let { it is FirResolvedDeclarationStatus } ?: true)
                check(declaration.setter?.status?.let { it is FirResolvedDeclarationStatus } ?: true)
            }
            is FirRegularClass -> check(declaration.status is FirResolvedDeclarationStatus)
            is FirAnonymousInitializer -> Unit
            else -> error("Unexpected type: ${declaration::class.simpleName}")
        }
    }
}
