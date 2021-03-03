/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.trasformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirImplicitAwareBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.ImplicitBodyResolveComputationSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.createReturnTypeCalculatorForIDE
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirIdeDesignatedBodyResolveTransformerForReturnTypeCalculator
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerDataContextCollector

internal class FirDesignatedImplicitTypesTransformerForIDE(
    designation: FirDesignation,
    session: FirSession,
    scopeSession: ScopeSession,
    private val towerDataContextCollector: FirTowerDataContextCollector?,
    implicitBodyResolveComputationSession: ImplicitBodyResolveComputationSession = ImplicitBodyResolveComputationSession(),
) : FirImplicitAwareBodyResolveTransformer(
    session,
    implicitBodyResolveComputationSession = implicitBodyResolveComputationSession,
    phase = FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE,
    implicitTypeOnly = true,
    scopeSession = scopeSession,
    returnTypeCalculator = createReturnTypeCalculatorForIDE(
        session,
        scopeSession,
        implicitBodyResolveComputationSession,
        ::FirIdeDesignatedBodyResolveTransformerForReturnTypeCalculator
    )
) {
    private val ideDeclarationTransformer = IDEDeclarationTransformer(designation)

    @Suppress("NAME_SHADOWING")
    override fun transformDeclarationContent(declaration: FirDeclaration, data: ResolutionMode): CompositeTransformResult<FirDeclaration> =
        ideDeclarationTransformer.transformDeclarationContent(this, declaration, data) { declaration, data ->
            super.transformDeclarationContent(declaration, data)
        }


    override fun needReplacePhase(firDeclaration: FirDeclaration): Boolean = ideDeclarationTransformer.needReplacePhase(firDeclaration)

    override fun onBeforeStatementResolution(statement: FirStatement) {
        towerDataContextCollector?.addStatementContext(statement, context.towerDataContext)
    }

    override fun onBeforeDeclarationContentResolve(declaration: FirDeclaration) {
        towerDataContextCollector?.addDeclarationContext(declaration, context.towerDataContext)
    }
}
