/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirProviderInterceptor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.*
import org.jetbrains.kotlin.idea.fir.low.level.api.FirPhaseRunner
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationDesignationWithFile
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirIdeDesignatedBodyResolveTransformerForReturnTypeCalculator
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.FirLazyTransformerForIDE.Companion.isResolvedForAllDeclarations
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.FirLazyTransformerForIDE.Companion.updateResolvedPhaseForDeclarationAndChildren
import org.jetbrains.kotlin.idea.fir.low.level.api.util.ensurePhase
import org.jetbrains.kotlin.idea.fir.low.level.api.util.ensurePhaseForClasses
import org.jetbrains.kotlin.idea.fir.low.level.api.util.isTargetCallableDeclarationAndInPhase

/**
 * Transform designation into BODY_RESOLVE declaration. Affects only for target declaration and it's children
 */
internal class FirDesignatedBodyResolveTransformerForIDE(
    private val designation: FirDeclarationDesignationWithFile,
    session: FirSession,
    scopeSession: ScopeSession,
    private val declarationPhaseDowngraded: Boolean,
    towerDataContextCollector: FirTowerDataContextCollector?,
    firProviderInterceptor: FirProviderInterceptor?,
) : FirLazyTransformerForIDE, FirBodyResolveTransformer(
    session,
    phase = FirResolvePhase.BODY_RESOLVE,
    implicitTypeOnly = false,
    scopeSession = scopeSession,
    returnTypeCalculator = createReturnTypeCalculatorForIDE(
        session,
        scopeSession,
        ImplicitBodyResolveComputationSession(),
        ::FirIdeDesignatedBodyResolveTransformerForReturnTypeCalculator
    ),
    firTowerDataContextCollector = towerDataContextCollector,
    firProviderInterceptor = firProviderInterceptor,
) {
    private val ideDeclarationTransformer = IDEDeclarationTransformer(designation)

    override fun transformDeclarationContent(declaration: FirDeclaration, data: ResolutionMode): FirDeclaration =
        ideDeclarationTransformer.transformDeclarationContent(this, declaration, data) {
            super.transformDeclarationContent(declaration, data)
        }

    override fun needReplacePhase(firDeclaration: FirDeclaration): Boolean =
        ideDeclarationTransformer.needReplacePhase && firDeclaration !is FirFile && super.needReplacePhase(firDeclaration)

    override fun transformDeclaration(phaseRunner: FirPhaseRunner) {
        if (designation.isResolvedForAllDeclarations(FirResolvePhase.BODY_RESOLVE, declarationPhaseDowngraded)) return
        designation.declaration.updateResolvedPhaseForDeclarationAndChildren(FirResolvePhase.BODY_RESOLVE)
        if (designation.isTargetCallableDeclarationAndInPhase(FirResolvePhase.BODY_RESOLVE)) return

        (designation.declaration as? FirCallableDeclaration<*>)?.ensurePhase(FirResolvePhase.CONTRACTS)
        designation.ensurePhaseForClasses(FirResolvePhase.STATUS)

        phaseRunner.runPhaseWithCustomResolve(FirResolvePhase.BODY_RESOLVE) {
            designation.firFile.transform<FirFile, ResolutionMode>(this, ResolutionMode.ContextIndependent)
        }

        ideDeclarationTransformer.ensureDesignationPassed()
        //TODO Figure out why the phase is not updated
        (designation.declaration as? FirTypeAlias)?.replaceResolvePhase(FirResolvePhase.BODY_RESOLVE)
        ensureResolved(designation.declaration)
        ensureResolvedDeep(designation.declaration)
    }

    override fun ensureResolved(declaration: FirDeclaration) {
        when (declaration) {
            is FirSimpleFunction, is FirConstructor, is FirTypeAlias, is FirField, is FirAnonymousInitializer ->
                declaration.ensurePhase(FirResolvePhase.BODY_RESOLVE)
            is FirProperty -> {
                declaration.ensurePhase(FirResolvePhase.BODY_RESOLVE)
                declaration.getter?.ensurePhase(FirResolvePhase.BODY_RESOLVE)
                declaration.setter?.ensurePhase(FirResolvePhase.BODY_RESOLVE)
            }
            is FirEnumEntry, is FirClass<*> -> Unit
            else -> error("Unexpected type: ${declaration::class.simpleName}")
        }
    }
}

