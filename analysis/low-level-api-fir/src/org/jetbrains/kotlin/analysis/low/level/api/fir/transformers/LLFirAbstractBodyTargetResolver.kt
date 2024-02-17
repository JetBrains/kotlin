/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.LLFirReturnTypeCalculatorWithJump
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyBodiesCalculator
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformerDispatcher
import org.jetbrains.kotlin.fir.visitors.transformSingle

internal abstract class LLFirAbstractBodyTargetResolver(
    resolveTarget: LLFirResolveTarget,
    resolvePhase: FirResolvePhase,
    protected val llImplicitBodyResolveComputationSession: LLImplicitBodyResolveComputationSession = LLImplicitBodyResolveComputationSession(),
    isJumpingPhase: Boolean = false,
) : LLFirTargetResolver(resolveTarget, resolvePhase, isJumpingPhase) {
    protected fun createReturnTypeCalculator(): LLFirReturnTypeCalculatorWithJump = LLFirReturnTypeCalculatorWithJump(
        resolveTargetScopeSession,
        llImplicitBodyResolveComputationSession,
    )

    abstract val transformer: FirAbstractBodyResolveTransformerDispatcher

    override fun checkResolveConsistency() {
        check(resolverPhase == transformer.transformerPhase) {
            "Inconsistent Resolver($resolverPhase) and Transformer(${transformer.transformerPhase}) phases"
        }
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withFile", level = DeprecationLevel.ERROR)
    override fun withContainingFile(firFile: FirFile, action: () -> Unit) {
        transformer.declarationsTransformer?.withFile(firFile) {
            action()
            firFile
        }
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withScript", level = DeprecationLevel.ERROR)
    override fun withContainingScript(firScript: FirScript, action: () -> Unit) {
        transformer.declarationsTransformer?.withScript(firScript) {
            action()
            firScript
        }
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withRegularClass", level = DeprecationLevel.ERROR)
    override fun withContainingRegularClass(firClass: FirRegularClass, action: () -> Unit) {
        transformer.declarationsTransformer?.context?.withContainingClass(firClass) {
            transformer.declarationsTransformer?.withRegularClass(firClass) {
                action()
                firClass
            }
        }
    }

    protected fun <T : FirElementWithResolveState> resolve(target: T, keeper: StateKeeper<T, FirDesignation>) {
        val firDesignation = FirDesignation(containingDeclarations, target)
        resolveWithKeeper(target, firDesignation, keeper, { FirLazyBodiesCalculator.calculateBodies(firDesignation) }) {
            rawResolve(target)
        }
    }

    protected open fun rawResolve(target: FirElementWithResolveState) {
        target.transformSingle(transformer, ResolutionMode.ContextIndependent)
    }

}
