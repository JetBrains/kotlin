/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase

abstract class FirPhaseManager : FirSessionComponent {
    abstract fun ensureResolved(
        symbol: FirBasedSymbol<*>,
        requiredPhase: FirResolvePhase = FirResolvePhase.DECLARATIONS
    )
}

val FirSession.phaseManager: FirPhaseManager by FirSession.sessionComponentAccessor()

fun FirBasedSymbol<*>.ensureResolved(requiredPhase: FirResolvePhase) {
    val session = fir.moduleData.session
    val phaseManager = session.phaseManager
    phaseManager.ensureResolved(this, requiredPhase)
}

fun FirDeclaration.ensureResolved(requiredPhase: FirResolvePhase) {
    symbol.ensureResolved(requiredPhase)
}
