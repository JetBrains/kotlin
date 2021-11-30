/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirBackingFieldSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.fir.utils.firRef
import org.jetbrains.kotlin.analysis.api.symbols.KtBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirModuleResolveState
import org.jetbrains.kotlin.fir.declarations.FirBackingField
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase

internal class KtFirBackingFieldSymbol(
    fir: FirBackingField,
    resolveState: FirModuleResolveState,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder
) : KtBackingFieldSymbol(), KtFirSymbol<FirBackingField> {
    override val firRef = firRef(fir, resolveState)

    override val origin: KtSymbolOrigin
        get() = withValidityAssertion { super<KtBackingFieldSymbol>.origin }

    override val returnType: KtType by cached {
        firRef.returnType(FirResolvePhase.TYPES, builder)
    }

    override val owningProperty: KtKotlinPropertySymbol by firRef.withFirAndCache { fir ->
        builder.buildSymbol(fir.propertySymbol.fir) as KtKotlinPropertySymbol
    }

    override fun createPointer(): KtSymbolPointer<KtBackingFieldSymbol> {
        return KtFirBackingFieldSymbolPointer(owningProperty.createPointer())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KtFirBackingFieldSymbol

        return this.firRef == other.firRef
    }

    override fun hashCode(): Int {
        return firRef.hashCode() * 31 + token.hashCode()
    }
}