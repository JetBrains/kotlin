/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.low.level.api.fir.ideSessionComponents
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.ir.util.IdSignature

internal data class FirCallableSignature(private val idSignature: IdSignature) {
    fun sameSignature(declaration: FirCallableDeclaration): Boolean = this == declaration.createSignature()
    fun sameSignature(declaration: FirCallableSymbol<*>): Boolean = sameSignature(declaration.fir)
}

internal fun FirCallableSymbol<*>.createSignature(): FirCallableSignature = fir.createSignature()

internal fun FirCallableDeclaration.createSignature(): FirCallableSignature {
    lazyResolveToPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
    val signatureComposer = moduleData.session.ideSessionComponents.signatureComposer
    return FirCallableSignature(
        signatureComposer.composeSignature(this, allowLocalClasses = true)
            ?: error("Could not compose signature for ${this.renderWithType()}, looks like it is private or local")
    )
}
