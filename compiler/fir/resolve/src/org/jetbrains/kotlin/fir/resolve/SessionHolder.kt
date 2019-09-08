/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.calls.InferenceComponents
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionStageRunner
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.phasedFir
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.FirSymbolOwner
import org.jetbrains.kotlin.fir.types.FirTypeRef

interface SessionHolder {
    val session: FirSession
}

interface BodyResolveComponents : SessionHolder {
    val returnTypeCalculator: ReturnTypeCalculator
    val implicitReceiverStack: ImplicitReceiverStack
    val noExpectedType: FirTypeRef
    val symbolProvider: FirSymbolProvider
    val file: FirFile
    val container: FirDeclaration
    val inferenceComponents: InferenceComponents
    val resolutionStageRunner: ResolutionStageRunner

    val <D> AbstractFirBasedSymbol<D>.phasedFir: D where D : FirDeclaration, D : FirSymbolOwner<D>
        get() = phasedFir(session, FirResolvePhase.DECLARATIONS)
}