/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.contracts.FirContractResolveTransformer
import org.jetbrains.kotlin.idea.fir.low.level.api.FirPhaseRunner
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationUntypedDesignationWithFile
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.FirLazyTransformerForIDE.Companion.isResolvedForAllDeclarations
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.FirLazyTransformerForIDE.Companion.updateResolvedPhaseForDeclarationAndChildren
import org.jetbrains.kotlin.idea.fir.low.level.api.util.*
import org.jetbrains.kotlin.idea.fir.low.level.api.util.ensurePhase

/**
 * Transform designation into CONTRACTS declaration. Affects only for target declaration and it's children
 */
internal class FirDesignatedContractsResolveTransformerForIDE(
    private val designation: FirDeclarationUntypedDesignationWithFile,
    session: FirSession,
    scopeSession: ScopeSession,
    private val declarationPhaseDowngraded: Boolean,
) : FirLazyTransformerForIDE, FirContractResolveTransformer(session, scopeSession) {

    private val ideDeclarationTransformer = IDEDeclarationTransformer(designation)

    override val declarationsTransformer: FirDeclarationsResolveTransformer = object : FirDeclarationsContractResolveTransformer(this) {
        override fun transformDeclarationContent(firClass: FirClass<*>, data: ResolutionMode) {
            ideDeclarationTransformer.transformDeclarationContent(this, firClass, data) {
                super.transformDeclarationContent(firClass, data)
                firClass
            }
        }
    }

    override fun transformDeclarationContent(declaration: FirDeclaration, data: ResolutionMode): FirDeclaration =
        ideDeclarationTransformer.transformDeclarationContent(this, declaration, data) {
            designation.declaration.updateResolvedPhaseForDeclarationAndChildren(FirResolvePhase.CONTRACTS)
            super.transformDeclarationContent(declaration, data)
        }

    override fun needReplacePhase(firDeclaration: FirDeclaration): Boolean =
        ideDeclarationTransformer.needReplacePhase && firDeclaration !is FirFile && super.needReplacePhase(firDeclaration)

    override fun transformDeclaration(phaseRunner: FirPhaseRunner) {
        if (designation.isResolvedForAllDeclarations(FirResolvePhase.CONTRACTS, declarationPhaseDowngraded)) return
        designation.declaration.updateResolvedPhaseForDeclarationAndChildren(FirResolvePhase.CONTRACTS)
        if (designation.isTargetCallableDeclarationAndInPhase(FirResolvePhase.CONTRACTS)) return
        designation.ensureDesignation(FirResolvePhase.STATUS)

        phaseRunner.runPhaseWithCustomResolve(FirResolvePhase.CONTRACTS) {
            designation.firFile.transform<FirFile, ResolutionMode>(this, ResolutionMode.ContextIndependent)
        }

        ideDeclarationTransformer.ensureDesignationPassed()
        ensureResolved(designation.declaration)
        ensureResolvedDeep(designation.declaration)
    }

    override fun ensureResolved(declaration: FirDeclaration) {
        when (declaration) {
            is FirSimpleFunction, is FirConstructor, is FirAnonymousInitializer ->
                declaration.ensurePhase(FirResolvePhase.CONTRACTS)
            is FirProperty -> {
                declaration.ensurePhase(FirResolvePhase.CONTRACTS)
                declaration.getter?.ensurePhase(FirResolvePhase.CONTRACTS)
                declaration.setter?.ensurePhase(FirResolvePhase.CONTRACTS)
            }
            is FirClass<*>, is FirTypeAlias, is FirEnumEntry, is FirField -> Unit
            else -> error("Unexpected type: ${declaration::class.simpleName}")
        }
    }
}
