/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.LLFirReturnTypeCalculatorWithJump
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyBodiesCalculator
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformerDispatcher
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.ImplicitBodyResolveComputationSession

internal abstract class LLFirAbstractBodyTargetResolver(
    resolveTarget: LLFirResolveTarget,
    lockProvider: LLFirLockProvider,
    private val scopeSession: ScopeSession,
    resolvePhase: FirResolvePhase,
    protected val implicitBodyResolveComputationSession: ImplicitBodyResolveComputationSession = ImplicitBodyResolveComputationSession(),
    isJumpingPhase: Boolean = false
) : LLFirTargetResolver(resolveTarget, lockProvider, resolvePhase, isJumpingPhase) {
    protected fun createReturnTypeCalculator(
        towerDataContextCollector: FirTowerDataContextCollector?,
    ): ReturnTypeCalculator = LLFirReturnTypeCalculatorWithJump(
        scopeSession,
        implicitBodyResolveComputationSession,
        lockProvider,
        towerDataContextCollector,
    )

    abstract val transformer: FirAbstractBodyResolveTransformerDispatcher

    override fun checkResolveConsistency() {
        check(resolverPhase == transformer.transformerPhase) {
            "Inconsistent Resolver($resolverPhase) and Transformer(${transformer.transformerPhase}) phases"
        }
    }

    override fun withFile(firFile: FirFile, action: () -> Unit) {
        transformer.context.withFile(firFile, transformer.components) {
            action()
        }
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withRegularClass", level = DeprecationLevel.ERROR)
    override fun withRegularClassImpl(firClass: FirRegularClass, action: () -> Unit) {
        transformer.declarationsTransformer.context.withContainingClass(firClass) {
            transformer.declarationsTransformer.withRegularClass(firClass) {
                action()
                firClass
            }
        }
    }

    protected fun calculateLazyBodies(declaration: FirElementWithResolveState) {
        val firDesignation = FirDesignationWithFile(nestedClassesStack, declaration, resolveTarget.firFile)
        FirLazyBodiesCalculator.calculateLazyBodiesInside(firDesignation)
    }
}
