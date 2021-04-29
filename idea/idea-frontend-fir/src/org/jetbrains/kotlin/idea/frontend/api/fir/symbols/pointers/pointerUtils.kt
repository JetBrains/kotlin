/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers

import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.idea.fir.low.level.api.ideSessionComponents
import org.jetbrains.kotlin.ir.util.IdSignature

internal inline fun <reified D : FirDeclaration> FirScope.findDeclarationWithSignature(
    signature: IdSignature,
    firSession: FirSession,
    processor: FirScope.((AbstractFirBasedSymbol<*>) -> Unit) -> Unit
): D? {
    val signatureComposer = firSession.ideSessionComponents.signatureComposer
    var foundSymbol: D? = null
    processor { symbol ->
        val declaration = symbol.fir
        if (declaration is D && signatureComposer.composeSignature(declaration) == signature) {
            foundSymbol = declaration
        }
    }
    return foundSymbol
}

internal inline fun <reified D : FirDeclaration> Collection<FirCallableSymbol<*>>.findDeclarationWithSignatureBySymbols(
    signature: IdSignature,
    firSession: FirSession
): D? {
    val signatureComposer = firSession.ideSessionComponents.signatureComposer
    for (symbol in this) {
        val declaration = symbol.fir
        if (declaration is D && signatureComposer.composeSignature(declaration) == signature) {
            return declaration
        }
    }
    return null
}

internal fun FirDeclaration.createSignature(): IdSignature {
    val signatureComposer = moduleData.session.ideSessionComponents.signatureComposer
    return signatureComposer.composeSignature(this)
        ?: error("Could not compose signature for ${this.renderWithType(FirRenderer.RenderMode.WithResolvePhases)}, looks like it is private or local")
}
