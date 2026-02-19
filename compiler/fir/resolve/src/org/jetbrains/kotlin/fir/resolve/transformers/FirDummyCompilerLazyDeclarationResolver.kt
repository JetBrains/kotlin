/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.FirLazyDeclarationResolver

object FirDummyCompilerLazyDeclarationResolver : FirLazyDeclarationResolver() {
    override fun startResolvingPhase(phase: FirResolvePhase) {}
    override fun finishResolvingPhase(phase: FirResolvePhase) {}

    override fun lazyResolveToPhase(element: FirElementWithResolveState, toPhase: FirResolvePhase) {}
    override fun lazyResolveToPhaseWithCallableMembers(clazz: FirClass, toPhase: FirResolvePhase) {}
    override fun lazyResolveToPhaseRecursively(element: FirElementWithResolveState, toPhase: FirResolvePhase) {}
}
