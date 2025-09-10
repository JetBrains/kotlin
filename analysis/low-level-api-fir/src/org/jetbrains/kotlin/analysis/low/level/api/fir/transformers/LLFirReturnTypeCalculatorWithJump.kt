/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.asResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirImplicitBodyTargetResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirImplicitTypesLazyResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLImplicitBodyResolveComputationSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.shouldBeResolvedOnImplicitTypePhase
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkCanceled
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.ReturnTypeCalculatorWithJump
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef

internal class LLFirReturnTypeCalculatorWithJump(
    scopeSession: ScopeSession,
    implicitBodyResolveComputationSession: LLImplicitBodyResolveComputationSession,
) : ReturnTypeCalculatorWithJump(scopeSession, implicitBodyResolveComputationSession) {
    override fun resolveDeclaration(declaration: FirCallableDeclaration): FirResolvedTypeRef {
        // Should be in sync with LLFirImplicitBodyTargetResolver
        val hasSomethingToResolveOnImplicitTypePhase = when {
            declaration is FirProperty -> declaration.shouldBeResolvedOnImplicitTypePhase
            else -> declaration.returnTypeRef is FirImplicitTypeRef
        }

        if (!hasSomethingToResolveOnImplicitTypePhase) {
            return declaration.symbol.resolvedReturnTypeRef
        }

        declaration.lazyResolveToPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE.previous)

        val designation = declaration.collectDesignation().asResolveTarget()
        val computationSession = implicitBodyResolveComputationSession as LLImplicitBodyResolveComputationSession
        val resolver = LLFirImplicitBodyTargetResolver(
            designation,
            llImplicitBodyResolveComputationSessionParameter = computationSession,
        )

        resolver.resolveDesignation()

        // Report recursion error if we found cycle during resolution
        if (computationSession.popCycledSymbolIfExists() == declaration.symbol) {
            return recursionInImplicitTypeRef(declaration)
        }

        LLFirImplicitTypesLazyResolver.checkIsResolved(declaration)
        return declaration.returnTypeRef as FirResolvedTypeRef
    }

    override fun tryCalculateReturnTypeOrNull(declaration: FirCallableDeclaration): FirResolvedTypeRef {
        checkCanceled()
        return super.tryCalculateReturnTypeOrNull(declaration)
    }
}
