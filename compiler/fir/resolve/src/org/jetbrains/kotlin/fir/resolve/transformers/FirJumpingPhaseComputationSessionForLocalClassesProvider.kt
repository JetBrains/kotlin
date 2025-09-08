/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.CompilerRequiredAnnotationsComputationSession

/**
 * This provider is required to avoid contact violations caused by CLI transformers which may jump from local
 * classes to other non-local declarations and incorrectly modify them in the Analysis API mode
 *
 * @see org.jetbrains.kotlin.fir.declarations.FirResolvePhase
 * @see org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.runAllPhasesForLocalClassLikeDeclarations
 */
@FirImplementationDetail
abstract class FirJumpingPhaseComputationSessionForLocalClassesProvider : FirSessionComponent {
    abstract fun compilerRequiredAnnotationPhaseSession(): CompilerRequiredAnnotationsComputationSession
    abstract fun superTypesPhaseSession(): SupertypeComputationSession
    abstract fun statusPhaseSession(
        useSiteSession: FirSession,
        useSiteScopeSession: ScopeSession,
        designationMapForLocalClasses: Map<FirClassLikeDeclaration, FirClassLikeDeclaration?>,
    ): StatusComputationSession
}

@FirImplementationDetail
object FirCliJumpingPhaseComputationSessionForLocalClassesProvider : FirJumpingPhaseComputationSessionForLocalClassesProvider() {
    override fun compilerRequiredAnnotationPhaseSession(): CompilerRequiredAnnotationsComputationSession {
        return CompilerRequiredAnnotationsComputationSession()
    }

    override fun superTypesPhaseSession(): SupertypeComputationSession {
        return SupertypeComputationSession()
    }

    override fun statusPhaseSession(
        useSiteSession: FirSession,
        useSiteScopeSession: ScopeSession,
        designationMapForLocalClasses: Map<FirClassLikeDeclaration, FirClassLikeDeclaration?>,
    ): StatusComputationSession = StatusComputationSession(
        useSiteSession,
        useSiteScopeSession,
        designationMapForLocalClasses,
    )
}

@OptIn(FirImplementationDetail::class)
internal val FirSession.jumpingPhaseComputationSessionForLocalClassesProvider: FirJumpingPhaseComputationSessionForLocalClassesProvider by FirSession.sessionComponentAccessor()
