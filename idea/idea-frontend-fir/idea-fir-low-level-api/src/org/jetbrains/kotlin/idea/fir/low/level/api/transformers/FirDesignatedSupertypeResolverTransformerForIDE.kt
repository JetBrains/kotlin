/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirApplySupertypesTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.FirProviderInterceptor
import org.jetbrains.kotlin.fir.resolve.transformers.FirSupertypeResolverVisitor
import org.jetbrains.kotlin.fir.resolve.transformers.SupertypeComputationSession
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationUntypedDesignationWithFile
import org.jetbrains.kotlin.idea.fir.low.level.api.util.ensurePathPhase
import org.jetbrains.kotlin.idea.fir.low.level.api.util.ensureTargetPhaseIfClass

internal class FirDesignatedSupertypeResolverTransformerForIDE(
    private val designation: FirDeclarationUntypedDesignationWithFile,
    private val session: FirSession,
    private val scopeSession: ScopeSession,
    private val firProviderInterceptor: FirProviderInterceptor?
) : FirLazyTransformerForIDE {

    override fun transformDeclaration() {
        if (designation.declaration.resolvePhase >= FirResolvePhase.SUPER_TYPES) return
        designation.ensurePathPhase(FirResolvePhase.SUPER_TYPES)

        val supertypeComputationSession = SupertypeComputationSession()

        val resolver = FirSupertypeResolverVisitor(
            session = session,
            supertypeComputationSession = supertypeComputationSession,
            scopeSession = scopeSession,
            scopeForLocalClass = null,
            localClassesNavigationInfo = null,
            firProviderInterceptor = firProviderInterceptor,
        )
        designation.declaration.accept(resolver, null)
        val applySupertypesTransformer = FirApplySupertypesTransformer(supertypeComputationSession)
        designation.declaration.transform<FirElement, Void?>(applySupertypesTransformer, null)
        designation.ensureTargetPhaseIfClass(FirResolvePhase.SUPER_TYPES)
    }
}
