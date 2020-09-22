/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.FirRefWithValidityCheck
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.overriddenDeclaration
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbolOrigin

internal interface KtFirSymbol<F : FirDeclaration> : KtSymbol, ValidityTokenOwner {
    val firRef: FirRefWithValidityCheck<F>

    override val origin: KtSymbolOrigin get() = firRef.withFir { it.ktSymbolOrigin() }
}


private tailrec fun FirDeclaration.ktSymbolOrigin(): KtSymbolOrigin = when (origin) {
    FirDeclarationOrigin.Source -> KtSymbolOrigin.SOURCE
    FirDeclarationOrigin.Library -> KtSymbolOrigin.LIBRARY
    FirDeclarationOrigin.Java -> KtSymbolOrigin.JAVA
    FirDeclarationOrigin.SamConstructor -> KtSymbolOrigin.SAM_CONSTRUCTOR
    FirDeclarationOrigin.Enhancement -> KtSymbolOrigin.JAVA
    else -> {
        val overridden = overriddenDeclaration ?: throw InvalidFirDeclarationOriginForSymbol(origin)
        overridden.ktSymbolOrigin()
    }
}

class InvalidFirDeclarationOriginForSymbol(origin: FirDeclarationOrigin) :
    IllegalStateException("Invalid FirDeclarationOrigin  $origin")