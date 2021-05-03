/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.trasformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirApplySupertypesTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.FirProviderInterceptorForSupertypeResolver
import org.jetbrains.kotlin.fir.resolve.transformers.FirSupertypeResolverVisitor
import org.jetbrains.kotlin.fir.resolve.transformers.SupertypeComputationSession
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationDesignation
import org.jetbrains.kotlin.idea.fir.low.level.api.trasformers.FirLazyTransformerForIDE.Companion.ensurePhase

internal class FirDesignatedSupertypeResolverTransformerForIDE(
    private val designation: FirDeclarationDesignation,
    private val session: FirSession,
    private val scopeSession: ScopeSession,
    private val firProviderInterceptor: FirProviderInterceptorForSupertypeResolver?,
) : FirLazyTransformerForIDE {

    private val supertypeComputationSession = SupertypeComputationSession()

    override fun transformDeclaration() {
        designation.ensurePhase(FirResolvePhase.SUPER_TYPES, exceptLast = true)

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
    }
}