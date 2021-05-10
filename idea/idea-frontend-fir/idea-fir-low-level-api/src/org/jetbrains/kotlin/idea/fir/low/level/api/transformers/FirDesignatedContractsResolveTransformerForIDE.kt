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
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationUntypedDesignationWithFile
import org.jetbrains.kotlin.idea.fir.low.level.api.util.ensureTargetPhase

internal class FirDesignatedContractsResolveTransformerForIDE(
    private val designation: FirDeclarationUntypedDesignationWithFile,
    session: FirSession,
    scopeSession: ScopeSession,
) : FirLazyTransformerForIDE, FirContractResolveTransformer(session, scopeSession) {
    private val ideDeclarationTransformer = IDEDeclarationTransformer(designation)

    override val declarationsTransformer: FirDeclarationsResolveTransformer = object : FirDeclarationsContractResolveTransformer(this) {
        override fun transformDeclarationContent(firClass: FirClass<*>, data: ResolutionMode) {
            ideDeclarationTransformer.transformDeclarationContent(this, firClass, data) {
                super.transformDeclarationContent(firClass, data)
            }
        }
    }


    @Suppress("NAME_SHADOWING")
    override fun transformDeclarationContent(declaration: FirDeclaration, data: ResolutionMode): FirDeclaration =
        ideDeclarationTransformer.transformDeclarationContent(this, declaration, data) {
            super.transformDeclarationContent(declaration, data)
        }

    override fun needReplacePhase(firDeclaration: FirDeclaration): Boolean = ideDeclarationTransformer.needReplacePhase

    override fun transformDeclaration() {
        if (designation.declaration.resolvePhase >= FirResolvePhase.CONTRACTS) return
        val typeAlias = designation.declaration as? FirTypeAlias
        if (typeAlias != null) {
            //Nothing to do with typealias to CONTRACTS
            typeAlias.replaceResolvePhase(FirResolvePhase.CONTRACTS)
            return
        }
        designation.ensureTargetPhase(FirResolvePhase.STATUS)
        designation.firFile.transform<FirFile, ResolutionMode>(this, ResolutionMode.ContextIndependent)
        ideDeclarationTransformer.ensureDesignationPassed()
        designation.ensureTargetPhase(FirResolvePhase.CONTRACTS)
    }
}
