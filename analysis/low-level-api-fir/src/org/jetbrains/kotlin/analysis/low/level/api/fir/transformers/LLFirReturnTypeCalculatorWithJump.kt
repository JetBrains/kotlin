/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.asResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirImplicitBodyTargetResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirImplicitTypesLazyResolver
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirResolveContextCollector
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.ImplicitBodyResolveComputationSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.ReturnTypeCalculatorWithJump
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef

internal class LLFirReturnTypeCalculatorWithJump(
    scopeSession: ScopeSession,
    implicitBodyResolveComputationSession: ImplicitBodyResolveComputationSession,
    private val lockProvider: LLFirLockProvider,
    private val towerDataContextCollector: FirResolveContextCollector?,
) : ReturnTypeCalculatorWithJump(scopeSession, implicitBodyResolveComputationSession) {
    override fun resolveDeclaration(declaration: FirCallableDeclaration): FirResolvedTypeRef {
        if (declaration.returnTypeRef !is FirImplicitTypeRef) {
            return declaration.symbol.resolvedReturnTypeRef
        }

        declaration.lazyResolveToPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE.previous)

        val designation = declaration.collectDesignationWithFile().asResolveTarget()
        val resolver = LLFirImplicitBodyTargetResolver(
            designation,
            lockProvider = lockProvider,
            session = declaration.moduleData.session,
            scopeSession = scopeSession,
            firResolveContextCollector = towerDataContextCollector,
            implicitBodyResolveComputationSession = implicitBodyResolveComputationSession,
        )

        lockProvider.withGlobalPhaseLock(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) {
            resolver.resolveDesignation()
        }

        LLFirImplicitTypesLazyResolver.checkIsResolved(designation)
        return declaration.returnTypeRef as FirResolvedTypeRef
    }
}
