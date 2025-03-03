/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.hasRegularSetter
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBasePropertySetterSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class KaFirPropertySetterSymbol(
    override val owningKaProperty: KaFirKotlinPropertySymbol<KtProperty>,
) : KaPropertySetterSymbol(), KaFirBasePropertySetterSymbol {
    init {
        requireWithAttachment(
            backingPsi?.property?.hasRegularSetter != false,
            { "Property setter without a body" },
        ) {
            withPsiEntry("propertySetter", backingPsi)
            withFirSymbolEntry("firSymbol", firSymbol)
        }
    }

    override val isExpect: Boolean
        get() = isExpectImpl

    override val isDefault: Boolean
        get() = isDefaultImpl

    override val isInline: Boolean
        get() = isInlineImpl

    override val isOverride: Boolean
        get() = isOverrideImpl

    override val hasBody: Boolean
        get() = hasBodyImpl

    override val modality: KaSymbolModality
        get() = modalityImpl

    override val compilerVisibility: Visibility
        get() = compilerVisibilityImpl

    override val annotations: KaAnnotationList
        get() = annotationsImpl

    override val callableId: CallableId?
        get() = callableIdImpl

    override val parameter: KaValueParameterSymbol
        get() = parameterImpl

    override val returnType: KaType
        get() = returnTypeImpl

    override val receiverParameter: KaReceiverParameterSymbol?
        get() = receiverParameterImpl

    override val hasStableParameterNames: Boolean
        get() = hasStableParameterNamesImpl

    override fun createPointer(): KaSymbolPointer<KaPropertySetterSymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<KaPropertySetterSymbol>()
            ?: KaBasePropertySetterSymbolPointer(owningKaProperty.createPointer(), this)
    }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()

    companion object {
        fun create(declaration: KtPropertyAccessor, session: KaFirSession): KaPropertySetterSymbol {
            val property = declaration.property
            val owningKaProperty = with(session) {
                @Suppress("UNCHECKED_CAST")
                property.symbol as KaFirKotlinPropertySymbol<KtProperty>
            }

            return if (property.hasRegularSetter) {
                KaFirPropertySetterSymbol(owningKaProperty)
            } else {
                KaFirDefaultPropertySetterSymbol(owningKaProperty)
            }
        }
    }
}
