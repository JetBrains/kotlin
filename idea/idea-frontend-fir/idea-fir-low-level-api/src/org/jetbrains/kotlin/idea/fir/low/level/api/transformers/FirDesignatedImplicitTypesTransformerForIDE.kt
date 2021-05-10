/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirImplicitAwareBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.ImplicitBodyResolveComputationSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.createReturnTypeCalculatorForIDE
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationUntypedDesignationWithFile
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirIdeDesignatedBodyResolveTransformerForReturnTypeCalculator
import org.jetbrains.kotlin.idea.fir.low.level.api.util.ensureTargetPhase

internal class FirDesignatedImplicitTypesTransformerForIDE(
    private val designation: FirDeclarationUntypedDesignationWithFile,
    session: FirSession,
    scopeSession: ScopeSession,
    towerDataContextCollector: FirTowerDataContextCollector? = null,
    implicitBodyResolveComputationSession: ImplicitBodyResolveComputationSession = ImplicitBodyResolveComputationSession(),
) : FirLazyTransformerForIDE, FirImplicitAwareBodyResolveTransformer(
    session,
    implicitBodyResolveComputationSession = implicitBodyResolveComputationSession,
    phase = FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE,
    implicitTypeOnly = true,
    scopeSession = scopeSession,
    firTowerDataContextCollector = towerDataContextCollector,
    returnTypeCalculator = createReturnTypeCalculatorForIDE(
        session,
        scopeSession,
        implicitBodyResolveComputationSession,
        ::FirIdeDesignatedBodyResolveTransformerForReturnTypeCalculator
    )
) {
    private val ideDeclarationTransformer = IDEDeclarationTransformer(designation)

    @Suppress("NAME_SHADOWING")
    override fun transformDeclarationContent(declaration: FirDeclaration, data: ResolutionMode): FirDeclaration =
        ideDeclarationTransformer.transformDeclarationContent(this, declaration, data) {
            super.transformDeclarationContent(declaration, data)
        }

    override fun needReplacePhase(firDeclaration: FirDeclaration): Boolean = ideDeclarationTransformer.needReplacePhase

    override fun transformDeclaration() {
        if (designation.declaration.resolvePhase >= FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) return
        designation.ensureTargetPhase(FirResolvePhase.CONTRACTS)

        when (val declaration = designation.declaration) {
            is FirCallableDeclaration<*> -> {
                //We don't need resolve callable declaration if it is already resolved (for ex. with TYPES)
                if (declaration.returnTypeRef is FirResolvedTypeRef) {
                    declaration.replaceResolvePhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
                    return
                }
            }
            is FirTypeAlias -> {
                //Nothing to do with type alias to this phase
                declaration.replaceResolvePhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
                return
            }
        }

        designation.firFile.transform<FirFile, ResolutionMode>(this, ResolutionMode.ContextIndependent)
        ideDeclarationTransformer.ensureDesignationPassed()

        val callableDeclaration = designation.declaration as? FirCallableDeclaration<*>
        check(callableDeclaration == null || callableDeclaration.returnTypeRef is FirResolvedTypeRef) {
            "Callable declaration seems to be unresolved after ${FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE} phase"
        }

        designation.ensureTargetPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
    }
}
