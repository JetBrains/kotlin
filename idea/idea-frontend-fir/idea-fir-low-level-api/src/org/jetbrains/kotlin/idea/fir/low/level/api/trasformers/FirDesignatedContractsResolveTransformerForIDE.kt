/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.trasformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.contracts.FirContractResolveTransformer
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult

internal class FirDesignatedContractsResolveTransformerForIDE(
    designation: FirDesignation,
    session: FirSession,
    scopeSession: ScopeSession,
) : FirContractResolveTransformer(session, scopeSession) {
    private val ideDeclarationTransformer = IDEDeclarationTransformer(designation)

    @Suppress("NAME_SHADOWING")
    override fun transformDeclarationContent(declaration: FirDeclaration, data: ResolutionMode): CompositeTransformResult<FirDeclaration> =
        ideDeclarationTransformer.transformDeclarationContent(this, declaration, data) { declaration, data ->
            super.transformDeclarationContent(declaration, data)
        }


    override fun needReplacePhase(firDeclaration: FirDeclaration): Boolean = ideDeclarationTransformer.needReplacePhase(firDeclaration)
}
