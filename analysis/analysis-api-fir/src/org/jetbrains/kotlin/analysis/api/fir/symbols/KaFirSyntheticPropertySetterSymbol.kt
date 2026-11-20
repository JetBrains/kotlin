/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirPropertySetterSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createOwnerPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal class KaFirSyntheticPropertySetterSymbol(
    internal val owningKaProperty: KaFirSyntheticJavaPropertySymbol,
) : KaPropertySetterSymbol(), KaFirSymbol<FirSyntheticPropertyAccessorSymbol> {
    override val firSymbol: FirSyntheticPropertyAccessorSymbol
        get() {
            val propertySymbol = owningKaProperty.firSymbol
            return propertySymbol.setterSymbol ?: errorWithAttachment("Setter is not found") {
                withFirSymbolEntry("property", propertySymbol)
            }
        }

    override val analysisSession: KaFirSession
        get() = owningKaProperty.analysisSession

    override val isNotDefault: Boolean
        get() = withValidityAssertion { true }

    @Deprecated("Use `!isNotDefault` instead", replaceWith = ReplaceWith("!isNotDefault"))
    override val isDefault: Boolean
        get() = withValidityAssertion { false }

    override val isInline: Boolean
        get() = withValidityAssertion { false }

    override val isExpect: Boolean
        get() = withValidityAssertion { false }

    override val isOverride: Boolean
        get() = withValidityAssertion {
            firSymbol.isOverride || firSymbol.fir.propertySymbol.isOverride
        }

    override val isExtension: Boolean
        get() = withValidityAssertion { firSymbol.isExtension }

    @Deprecated("Use `isNotDefault` instead", replaceWith = ReplaceWith("isNotDefault"))
    override val hasBody: Boolean
        get() = withValidityAssertion { firSymbol.fir.hasBody }

    override val modality: KaSymbolModality
        get() = withValidityAssertion { firSymbol.kaSymbolModality }

    override val visibility: KaSymbolVisibility
        get() = withValidityAssertion { firSymbol.visibility.asKaSymbolVisibility }

    override val returnType: KaType
        get() = withValidityAssertion { firSymbol.returnType(builder) }

    override val receiverParameter: KaReceiverParameterSymbol?
        get() = withValidityAssertion { KaFirReceiverParameterSymbol.create(null, analysisSession, this) }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion {
            KaFirAnnotationListForDeclaration.create(firSymbol, builder)
        }

    override val callableId: CallableId?
        get() = withValidityAssertion { firSymbol.delegateFunctionSymbol.callableId }

    override val parameter: KaValueParameterSymbol
        get() = withValidityAssertion { firSymbol.createKtValueParameters(builder).single() }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { firSymbol.fir.hasStableParameterNames }

    override val isExternal: Boolean
        get() = withValidityAssertion { firSymbol.isEffectivelyExternal(analysisSession.firSession) }

    override fun createPointer(): KaSymbolPointer<KaPropertySetterSymbol> = withValidityAssertion {
        KaFirPropertySetterSymbolPointer(propertySymbolPointer = createOwnerPointer(), this)
    }

    /**
     * Synthetic property accessors are built anew together with their property, so two symbols of the same setter are compared by their
     * owning property.
     */
    override fun equals(other: Any?): Boolean = this === other ||
            other is KaFirSyntheticPropertySetterSymbol &&
            other.owningKaProperty == owningKaProperty

    override fun hashCode(): Int = 31 * owningKaProperty.hashCode() + KaFirKotlinPropertySymbol.HASH_CODE_ADDITION_FOR_SETTER
}
