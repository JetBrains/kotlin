/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirBackingFieldSymbolPointer
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaBaseEmptyAnnotationList
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

/**
 * Note: current implementation doesn't support explicit backing field
 */
internal class KaFirBackingFieldSymbol private constructor(
    val owningKaProperty: KaFirKotlinPropertySymbol<*>,
) : KaBackingFieldSymbol(), KaFirSymbol<FirBackingFieldSymbol> {
    override val analysisSession: KaFirSession get() = owningKaProperty.analysisSession

    override val firSymbol: FirBackingFieldSymbol
        get() = owningKaProperty.firSymbol.backingFieldSymbol ?: errorWithAttachment("Property has no backing field") {
            withFirSymbolEntry("property", owningKaProperty.firSymbol)
        }

    override val origin: KaSymbolOrigin
        get() = withValidityAssertion { super<KaBackingFieldSymbol>.origin }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion {
            if (owningKaProperty.backingPsi?.cannotHaveBackingFieldAnnotation() == true)
                KaBaseEmptyAnnotationList(token)
            else
                KaFirAnnotationListForDeclaration.create(firSymbol, builder)
        }

    override val returnType: KaType
        get() = withValidityAssertion { firSymbol.returnType(builder) }

    override val owningProperty: KaKotlinPropertySymbol
        get() = withValidityAssertion { owningKaProperty }

    override val isVal: Boolean
        get() = withValidityAssertion { owningKaProperty.isVal }

    override fun createPointer(): KaSymbolPointer<KaBackingFieldSymbol> = withValidityAssertion {
        KaFirBackingFieldSymbolPointer(owningKaProperty.createPointer(), this)
    }

    override fun equals(other: Any?): Boolean = this === other ||
            other is KaFirBackingFieldSymbol &&
            other.owningKaProperty == owningKaProperty

    override fun hashCode(): Int = 31 * owningKaProperty.hashCode() + KaFirKotlinPropertySymbol.HASH_CODE_ADDITION_FOR_BACKING_FIELD

    companion object {
        fun create(owningKaProperty: KaFirKotlinPropertySymbol<*>): KaFirBackingFieldSymbol? {
            if (owningKaProperty.backingPsi == null && owningKaProperty.firSymbol.backingFieldSymbol == null) {
                return null
            }

            return KaFirBackingFieldSymbol(owningKaProperty)
        }
    }
}

private fun KtAnnotated.cannotHaveBackingFieldAnnotation(): Boolean = annotationEntries.none {
    when (it.useSiteTarget?.getAnnotationUseSiteTarget()) {
        null, AnnotationUseSiteTarget.FIELD, AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD -> true
        else -> false
    }
}