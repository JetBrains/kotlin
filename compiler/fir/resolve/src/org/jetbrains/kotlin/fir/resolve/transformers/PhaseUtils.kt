/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol

fun <D> AbstractFirBasedSymbol<D>.phasedFir(
    requiredPhase: FirResolvePhase = FirResolvePhase.DECLARATIONS
): D where D : FirDeclaration, D : FirSymbolOwner<D> {
    val result = this.fir
    val resolver = fir.session.phaseManager
        ?: error("phasedFirFileResolver should be defined when working with FIR in phased mode")

    resolver.ensureResolved(this, requiredPhase)

    return result
}

