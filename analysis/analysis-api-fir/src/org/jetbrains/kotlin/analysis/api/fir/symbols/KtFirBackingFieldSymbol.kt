/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirModuleResolveState
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirBackingFieldSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.fir.utils.firRef
import org.jetbrains.kotlin.analysis.api.symbols.KtBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtType

internal class KtFirBackingFieldSymbol(
    propertyFir: FirProperty,
    resolveState: FirModuleResolveState,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder
) : KtBackingFieldSymbol() {
    private val propertyFirRef = firRef(propertyFir, resolveState)

    override val returnType: KtType by cached {
        propertyFirRef.returnType(FirResolvePhase.TYPES, builder)
    }

    override val owningProperty: KtKotlinPropertySymbol by propertyFirRef.withFirAndCache { fir ->
        builder.buildSymbol(fir) as KtKotlinPropertySymbol
    }

    override fun createPointer(): KtSymbolPointer<KtBackingFieldSymbol> {
        return KtFirBackingFieldSymbolPointer(owningProperty.createPointer())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KtFirBackingFieldSymbol

        if (this.token != other.token) return false
        return this.propertyFirRef == other.propertyFirRef
    }

    override fun hashCode(): Int {
        return propertyFirRef.hashCode() * 31 + token.hashCode()
    }
}