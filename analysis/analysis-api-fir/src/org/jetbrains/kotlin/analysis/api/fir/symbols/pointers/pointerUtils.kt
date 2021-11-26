/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.ideSessionComponents
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.ClassId

internal inline fun <reified D : FirDeclaration> FirScope.findDeclarationWithSignature(
    signature: IdSignature,
    firSession: FirSession,
    processor: FirScope.((FirBasedSymbol<*>) -> Unit) -> Unit
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

internal fun KtFirAnalysisSession.getClassLikeSymbol(classId: ClassId) =
    firResolveState.rootModuleSession.symbolProvider.getClassLikeSymbolByClassId(classId)?.fir
