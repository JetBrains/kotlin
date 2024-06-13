/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirBackingFieldSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol

internal class KaFirBackingFieldSymbol(
    override val firSymbol: FirBackingFieldSymbol,
    override val analysisSession: KaFirSession,
) : KaBackingFieldSymbol(), KaFirSymbol<FirBackingFieldSymbol> {
    override val origin: KaSymbolOrigin get() = withValidityAssertion { super<KaBackingFieldSymbol>.origin }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion {
            KaFirAnnotationListForDeclaration.create(firSymbol, builder)
        }

    override val returnType: KaType get() = withValidityAssertion { firSymbol.returnType(builder) }

    override val owningProperty: KaKotlinPropertySymbol
        get() = withValidityAssertion {
            builder.variableLikeBuilder.buildPropertySymbol(firSymbol.propertySymbol) as KaKotlinPropertySymbol
        }

    override fun createPointer(): KaSymbolPointer<KaBackingFieldSymbol> = withValidityAssertion {
        KaFirBackingFieldSymbolPointer(owningProperty.createPointer())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KaFirBackingFieldSymbol

        return this.firSymbol == other.firSymbol
    }

    override fun hashCode(): Int {
        return firSymbol.hashCode()
    }
}