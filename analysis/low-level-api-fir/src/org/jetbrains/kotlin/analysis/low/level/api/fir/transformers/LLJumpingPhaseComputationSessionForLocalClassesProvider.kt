/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirJumpingPhaseComputationSessionForLocalClassesProvider
import org.jetbrains.kotlin.fir.resolve.transformers.StatusComputationSession
import org.jetbrains.kotlin.fir.resolve.transformers.SupertypeComputationSession
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.CompilerRequiredAnnotationsComputationSession
import org.jetbrains.kotlin.fir.scopes.FirScope

@OptIn(FirImplementationDetail::class)
internal object LLJumpingPhaseComputationSessionForLocalClassesProvider : FirJumpingPhaseComputationSessionForLocalClassesProvider() {
    override fun compilerRequiredAnnotationPhaseSession(): CompilerRequiredAnnotationsComputationSession {
        return LLCompilerRequiredAnnotationsComputationSessionLocalClassesAware()
    }

    override fun superTypesPhaseSession(): SupertypeComputationSession {
        return LLSupertypeComputationSessionLocalClassesAware()
    }

    override fun statusPhaseSession(
        useSiteSession: FirSession,
        useSiteScopeSession: ScopeSession,
        designationMapForLocalClasses: Map<FirClassLikeDeclaration, FirClassLikeDeclaration?>,
        scopeForLocalClass: FirScope?
    ): StatusComputationSession = LLStatusComputationSessionLocalClassesAware(
        useSiteSession,
        useSiteScopeSession,
    )
}
