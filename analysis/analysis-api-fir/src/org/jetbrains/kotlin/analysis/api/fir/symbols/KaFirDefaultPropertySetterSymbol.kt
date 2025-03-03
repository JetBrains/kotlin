/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
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
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment

/**
 * Represents the default property setter for [KaFirKotlinPropertyKtParameterBasedSymbol] and [KaFirKotlinPropertyKtDestructuringDeclarationEntryBasedSymbol].
 *
 * Also represents [KaFirKotlinPropertyKtPropertyBasedSymbol] with backing [KtPropertyAccessor] without a body.
 */
internal class KaFirDefaultPropertySetterSymbol(
    override val owningKaProperty: KaFirKotlinPropertySymbol<*>,
) : KaPropertySetterSymbol(), KaFirBasePropertySetterSymbol {
    init {
        requireWithAttachment(
            backingPsi?.hasBody() != true,
            { "This implementation should not be created for property accessor with a body" },
        ) {
            withFirSymbolEntry("property", owningKaProperty.firSymbol)
        }
    }

    override val isExpect: Boolean
        get() = isExpectImpl

    override val isDefault: Boolean
        get() = withValidityAssertion { true }

    override val isInline: Boolean
        get() = isInlineImpl

    override val isOverride: Boolean
        get() = isOverrideImpl

    override val hasBody: Boolean
        get() = withValidityAssertion { false }

    override val modality: KaSymbolModality
        get() = modalityImpl

    override val compilerVisibility: Visibility
        get() = compilerVisibilityImpl

    override val returnType: KaType
        get() = returnTypeImpl

    override val receiverParameter: KaReceiverParameterSymbol?
        get() = receiverParameterImpl

    override val annotations: KaAnnotationList
        get() = annotationsImpl

    override val callableId: CallableId?
        get() = callableIdImpl

    override val hasStableParameterNames: Boolean
        get() = hasStableParameterNamesImpl

    override val parameter: KaValueParameterSymbol
        get() = parameterImpl

    override fun createPointer(): KaSymbolPointer<KaPropertySetterSymbol> = withValidityAssertion {
        KaBasePropertySetterSymbolPointer(owningKaProperty.createPointer(), this)
    }

    override fun equals(other: Any?): Boolean = this === other ||
            other is KaFirDefaultPropertySetterSymbol &&
            other.owningKaProperty == owningKaProperty

    override fun hashCode(): Int = 31 * owningKaProperty.hashCode() + KaFirKotlinPropertySymbol.HASH_CODE_ADDITION_FOR_SETTER
}
