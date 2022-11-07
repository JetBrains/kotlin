/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.ideSessionComponents
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal inline fun <reified D : FirDeclaration> FirScope.findDeclarationWithSignature(
    signature: IdSignature,
    firSession: FirSession,
    processor: FirScope.((FirBasedSymbol<*>) -> Unit) -> Unit
): D? {
    val signatureComposer = firSession.ideSessionComponents.signatureComposer
    var foundSymbol: D? = null
    processor { symbol ->
        val declaration = symbol.fir
        if (declaration is D && signatureComposer.composeSignature(declaration, allowLocalClasses = true) == signature) {
            foundSymbol = declaration
        }
    }

    return foundSymbol
}

internal inline fun <reified D : FirClassifierSymbol<*>> FirScope.findClassifier(name: Name): D? {
    var foundSymbol: D? = null
    processClassifiersByName(name) {
        if (it is D) {
            foundSymbol = it
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

internal fun FirBasedSymbol<*>.createSignature(): IdSignature = fir.createSignature()

internal fun FirDeclaration.createSignature(): IdSignature {
    lazyResolveToPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
    val signatureComposer = moduleData.session.ideSessionComponents.signatureComposer
    return signatureComposer.composeSignature(this, allowLocalClasses = true)
        ?: error("Could not compose signature for ${this.renderWithType()}, looks like it is private or local")
}

internal fun KtFirAnalysisSession.getClassLikeSymbol(classId: ClassId) =
    firResolveSession.useSiteFirSession.symbolProvider.getClassLikeSymbolByClassId(classId)?.fir
