/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol

abstract class FirPhaseManager : FirSessionComponent {
    abstract fun <D> ensureResolved(
        symbol: AbstractFirBasedSymbol<D>,
        requiredPhase: FirResolvePhase = FirResolvePhase.DECLARATIONS
    ) where D : FirDeclaration, D : FirSymbolOwner<D>
}

internal val FirSession.phaseManager: FirPhaseManager? by FirSession.nullableSessionComponentAccessor()
