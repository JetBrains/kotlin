/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.FirRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion

internal interface KtFirSymbol<F : FirDeclaration> : KtSymbol, ValidityTokenOwner {
    val firRef: FirRef<F>

    override val origin: KtSymbolOrigin get() = firRef.withFir { it.origin.asKtSymbolOrigin() }
}

internal fun FirDeclarationOrigin.asKtSymbolOrigin() = when (this) {
    FirDeclarationOrigin.Source -> KtSymbolOrigin.SOURCE
    FirDeclarationOrigin.Library -> KtSymbolOrigin.LIBRARY
    FirDeclarationOrigin.Java -> KtSymbolOrigin.JAVA
    FirDeclarationOrigin.SamConstructor -> KtSymbolOrigin.SAM_CONSTRUCTOR
    FirDeclarationOrigin.Synthetic -> throw InvalidFirDeclarationOriginForSymbol(this)
    FirDeclarationOrigin.FakeOverride -> throw InvalidFirDeclarationOriginForSymbol(this)
    FirDeclarationOrigin.ImportedFromObject -> throw InvalidFirDeclarationOriginForSymbol(this)
    FirDeclarationOrigin.IntersectionOverride -> throw InvalidFirDeclarationOriginForSymbol(this)
    FirDeclarationOrigin.Enhancement -> KtSymbolOrigin.JAVA // TODO
    is FirDeclarationOrigin.Plugin -> throw InvalidFirDeclarationOriginForSymbol(this)
}

class InvalidFirDeclarationOriginForSymbol(origin: FirDeclarationOrigin) :
    IllegalStateException("Invalid FirDeclarationOrigin  $origin")