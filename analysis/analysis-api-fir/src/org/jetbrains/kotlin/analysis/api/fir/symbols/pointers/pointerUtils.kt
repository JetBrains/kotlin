/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal inline fun <reified D : FirCallableDeclaration> FirScope.findDeclarationWithSignature(
    signature: FirCallableSignature,
    processor: FirScope.((FirBasedSymbol<*>) -> Unit) -> Unit,
): D? {
    var foundSymbol: D? = null
    processor { symbol ->
        val declaration = symbol.fir
        if (declaration is D && signature.hasTheSameSignature(declaration)) {
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

internal inline fun <reified D : FirCallableDeclaration> Collection<FirCallableSymbol<*>>.findDeclarationWithSignatureBySymbols(
    signature: FirCallableSignature,
): D? {
    for (symbol in this) {
        val declaration = symbol.fir
        if (declaration is D && signature.hasTheSameSignature(declaration)) {
            return declaration
        }
    }

    return null
}

internal fun KtFirAnalysisSession.getClassLikeSymbol(classId: ClassId) =
    useSiteSession.symbolProvider.getClassLikeSymbolByClassId(classId)?.fir
