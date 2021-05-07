/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirTypeResolveTransformer
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationDesignationWithFile
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.FirLazyTransformerForIDE.Companion.ensurePathPhase
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.FirLazyTransformerForIDE.Companion.ensureTargetPhase
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.FirLazyTransformerForIDE.Companion.ensureTargetPhaseIfClass

internal class FirDesignatedTypeResolverTransformerForIDE(
    private val designation: FirDeclarationDesignationWithFile,
    session: FirSession,
    scopeSession: ScopeSession,
) : FirLazyTransformerForIDE, FirTypeResolveTransformer(session, scopeSession) {

    private val ideDeclarationTransformer = IDEDeclarationTransformer(designation)

    override fun <E : FirElement> transformElement(element: E, data: Any?): E {
        if (element !is FirRegularClass && element !is FirAnonymousObject && element !is FirFile)
            return super.transformElement(element, data)

        return ideDeclarationTransformer.transformDeclarationContent(this, element, data) {
            super.transformElement(element, data)
        }
    }

    override fun transformDeclaration() {
        if (designation.declaration.resolvePhase >= FirResolvePhase.TYPES) return
        designation.ensurePathPhase(FirResolvePhase.TYPES)
        designation.ensureTargetPhaseIfClass(FirResolvePhase.SUPER_TYPES)
        designation.firFile.transform<FirFile, Any?>(this, null)
        ideDeclarationTransformer.ensureDesignationPassed()
        designation.ensureTargetPhase(FirResolvePhase.TYPES)
    }
}