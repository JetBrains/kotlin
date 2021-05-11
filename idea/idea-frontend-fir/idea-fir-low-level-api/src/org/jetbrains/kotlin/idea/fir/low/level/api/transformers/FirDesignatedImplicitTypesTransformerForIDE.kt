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
import org.jetbrains.kotlin.idea.fir.low.level.api.FirPhaseRunner
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationUntypedDesignationWithFile
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirIdeDesignatedBodyResolveTransformerForReturnTypeCalculator
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.FirLazyTransformerForIDE.Companion.isResolvedForAllDeclarations
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.FirLazyTransformerForIDE.Companion.updateResolvedPhaseForDeclarationAndChildren
import org.jetbrains.kotlin.idea.fir.low.level.api.util.ensurePhase
import org.jetbrains.kotlin.idea.fir.low.level.api.util.ensurePhaseForClasses
import org.jetbrains.kotlin.idea.fir.low.level.api.util.isTargetCallableDeclarationAndInPhase

/**
 * Transform designation into IMPLICIT_TYPES_BODY_RESOLVE declaration. Affects only for target declaration, it's children and dependents
 */
internal class FirDesignatedImplicitTypesTransformerForIDE(
    private val designation: FirDeclarationUntypedDesignationWithFile,
    session: FirSession,
    scopeSession: ScopeSession,
    private val declarationPhaseDowngraded: Boolean,
    towerDataContextCollector: FirTowerDataContextCollector?,
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

    override fun transformDeclarationContent(declaration: FirDeclaration, data: ResolutionMode): FirDeclaration =
        ideDeclarationTransformer.transformDeclarationContent(this, declaration, data) {
            super.transformDeclarationContent(declaration, data)
        }

    override fun needReplacePhase(firDeclaration: FirDeclaration): Boolean =
        ideDeclarationTransformer.needReplacePhase && firDeclaration !is FirFile && super.needReplacePhase(firDeclaration)

    override fun transformDeclaration(phaseRunner: FirPhaseRunner) {
        if (designation.isResolvedForAllDeclarations(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE, declarationPhaseDowngraded)) return
        designation.declaration.updateResolvedPhaseForDeclarationAndChildren(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
        if (designation.isTargetCallableDeclarationAndInPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)) return

        val callableDeclaration = designation.declaration as? FirCallableDeclaration<*>
        if (callableDeclaration != null) {
            if (callableDeclaration.returnTypeRef is FirResolvedTypeRef) return
            callableDeclaration.ensurePhase(FirResolvePhase.CONTRACTS)
        }
        designation.ensurePhaseForClasses(FirResolvePhase.STATUS)

        phaseRunner.runPhaseWithCustomResolve(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) {
            designation.firFile.transform<FirFile, ResolutionMode>(this, ResolutionMode.ContextIndependent)
        }

        ideDeclarationTransformer.ensureDesignationPassed()
        ensureResolved(designation.declaration)
        ensureResolvedDeep(designation.declaration)
    }

    override fun ensureResolved(declaration: FirDeclaration) {
        when (declaration) {
            is FirSimpleFunction -> check(declaration.returnTypeRef is FirResolvedTypeRef)
            is FirField -> check(declaration.returnTypeRef is FirResolvedTypeRef)
            is FirClass<*>, is FirConstructor, is FirTypeAlias, is FirEnumEntry, is FirAnonymousInitializer -> Unit
            is FirProperty -> {
                check(declaration.returnTypeRef is FirResolvedTypeRef)
                //Not resolved for some getters and setters #KT-46995
//                check(declaration.getter?.returnTypeRef?.let { it is FirResolvedTypeRef } ?: true)
//                check(declaration.setter?.returnTypeRef?.let { it is FirResolvedTypeRef } ?: true)
            }
            else -> error("Unexpected type: ${declaration::class.simpleName}")
        }
    }
}
