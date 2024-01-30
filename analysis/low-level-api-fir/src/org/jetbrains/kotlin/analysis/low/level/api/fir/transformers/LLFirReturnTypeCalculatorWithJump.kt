/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.asResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirImplicitBodyTargetResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirImplicitTypesLazyResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLImplicitBodyResolveComputationSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirResolveContextCollector
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.ReturnTypeCalculatorWithJump
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef

internal class LLFirReturnTypeCalculatorWithJump(
    scopeSession: ScopeSession,
    implicitBodyResolveComputationSession: LLImplicitBodyResolveComputationSession,
    private val lockProvider: LLFirLockProvider,
    private val towerDataContextCollector: FirResolveContextCollector?,
) : ReturnTypeCalculatorWithJump(scopeSession, implicitBodyResolveComputationSession) {
    override fun resolveDeclaration(declaration: FirCallableDeclaration): FirResolvedTypeRef {
        if (declaration.returnTypeRef !is FirImplicitTypeRef) {
            return declaration.symbol.resolvedReturnTypeRef
        }

        declaration.lazyResolveToPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE.previous)

        val designation = declaration.collectDesignationWithFile().asResolveTarget()
        val targetSession = designation.target.llFirSession
        val computationSession = implicitBodyResolveComputationSession as LLImplicitBodyResolveComputationSession
        val resolver = LLFirImplicitBodyTargetResolver(
            designation,
            lockProvider = lockProvider,
            scopeSession = targetSession.getScopeSession(),
            firResolveContextCollector = towerDataContextCollector,
            llImplicitBodyResolveComputationSessionParameter = computationSession,
        )

        lockProvider.withGlobalPhaseLock(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) {
            resolver.resolveDesignation()
        }

        // Report recursion error if we found cycle during resolution
        if (computationSession.popCycledSymbolIfExists() == declaration.symbol) {
            return recursionInImplicitTypeRef()
        }

        LLFirImplicitTypesLazyResolver.checkIsResolved(designation)
        return declaration.returnTypeRef as FirResolvedTypeRef
    }
}
