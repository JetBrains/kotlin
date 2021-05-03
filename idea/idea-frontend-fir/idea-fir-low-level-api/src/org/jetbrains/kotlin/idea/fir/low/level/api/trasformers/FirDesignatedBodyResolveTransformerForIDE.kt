/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.trasformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirProviderInterceptorForSupertypeResolver
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.*
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationDesignation
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirIdeDesignatedBodyResolveTransformerForReturnTypeCalculator

internal class FirDesignatedBodyResolveTransformerForIDE(
    private val firFile: FirFile,
    designation: FirDeclarationDesignation,
    session: FirSession,
    scopeSession: ScopeSession,
    towerDataContextCollector: FirTowerDataContextCollector? = null,
    firProviderInterceptor: FirProviderInterceptorForSupertypeResolver? = null,
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
    private val ideDeclarationTransformer = IDEDeclarationTransformer(designation.toDesignationIterator())

    @Suppress("NAME_SHADOWING")
    override fun transformDeclarationContent(declaration: FirDeclaration, data: ResolutionMode): FirDeclaration =
        ideDeclarationTransformer.transformDeclarationContent(this, declaration, data) { declaration, data ->
            super.transformDeclarationContent(declaration, data)
        }

    override fun needReplacePhase(firDeclaration: FirDeclaration): Boolean =
        ideDeclarationTransformer.needReplacePhase(firDeclaration)

    override fun transformDeclaration() {
        firFile.transform<FirFile, ResolutionMode>(this, ResolutionMode.ContextDependent)
    }
}

