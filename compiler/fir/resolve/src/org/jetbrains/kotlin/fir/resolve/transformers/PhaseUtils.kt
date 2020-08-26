/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol

fun <D> AbstractFirBasedSymbol<D>.phasedFir(
    requiredPhase: FirResolvePhase = FirResolvePhase.DECLARATIONS
): D where D : FirDeclaration, D : FirSymbolOwner<D> {

    ensureResolved(requiredPhase, fir.session)

    return fir
}

fun <D> AbstractFirBasedSymbol<D>.ensureResolved(
    requiredPhase: FirResolvePhase,
    // TODO: Currently, the parameter is unused but it's needed to guarantee that all call-sites are able to supply use-site session
    // TODO: Decide which one session should be used and probably get rid of the parameter if use-site session is not needed
    useSiteSession: FirSession,
) where D : FirDeclaration, D : FirSymbolOwner<D> {
    val availablePhase = fir.resolvePhase
    if (availablePhase >= requiredPhase) return
    val resolver = fir.session.phaseManager
        ?: error("phaseManager should be defined when working with FIR in phased mode")

    resolver.ensureResolved(this, requiredPhase)
}
