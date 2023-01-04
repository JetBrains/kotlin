/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirDesignationToResolve
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.tryCollectDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyBodiesCalculator
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformerDispatcher

internal abstract class LLFirAbstractBodyResolver(
    designation: LLFirDesignationToResolve,
    lockProvider: LLFirLockProvider,
    resolvePhase: FirResolvePhase,
) : LLFirAbstractMultiDesignationResolver(designation, lockProvider, resolvePhase) {

    abstract val transformer: FirAbstractBodyResolveTransformerDispatcher

    override fun checkResolveConsistency() {
        check(phase == transformer.transformerPhase) {
            "Inconsistent Resolver($phase) and Transformer(${transformer.transformerPhase}) phases"
        }
    }

    override fun withFile(firFile: FirFile, action: () -> Unit) {
        transformer.context.withFile(firFile, transformer.components) {
            action()
        }
    }

    override fun withRegularClass(firClass: FirRegularClass, action: () -> Unit) {
        transformer.declarationsTransformer.resolveRegularClass(firClass) {
            action()
            firClass
        }
    }

    protected fun calculateLazyBodies(declaration: FirElementWithResolveState) {
        val firDesignation = declaration.tryCollectDesignationWithFile() ?: return
        FirLazyBodiesCalculator.calculateLazyBodiesInside(firDesignation)
    }
}
