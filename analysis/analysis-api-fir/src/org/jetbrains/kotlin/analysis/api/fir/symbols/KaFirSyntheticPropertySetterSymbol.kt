/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.entryPointImplementation
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirPropertySetterSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createOwnerPointer
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
        get() = entryPointImplementation { true }

    @Deprecated("Use `!isNotDefault` instead", replaceWith = ReplaceWith("!isNotDefault"))
    override val isDefault: Boolean
        get() = entryPointImplementation { false }

    override val isInline: Boolean
        get() = entryPointImplementation { false }

    override val isExpect: Boolean
        get() = entryPointImplementation { false }

    override val isOverride: Boolean
        get() = entryPointImplementation {
            firSymbol.isOverride || firSymbol.fir.propertySymbol.isOverride
        }

    override val isExtension: Boolean
        get() = entryPointImplementation { firSymbol.isExtension }

    @Deprecated("Use `isNotDefault` instead", replaceWith = ReplaceWith("isNotDefault"))
    override val hasBody: Boolean
        get() = entryPointImplementation { firSymbol.fir.hasBody }

    override val modality: KaSymbolModality
        get() = entryPointImplementation { firSymbol.kaSymbolModality }

    override val visibility: KaSymbolVisibility
        get() = entryPointImplementation { firSymbol.visibility.asKaSymbolVisibility }

    override val returnType: KaType
        get() = entryPointImplementation { firSymbol.returnType() }

    override val receiverParameter: KaReceiverParameterSymbol?
        get() = entryPointImplementation { KaFirReceiverParameterSymbol.create(null, this, analysisSession = analysisSession) }

    override val annotations: KaAnnotationList
        get() = entryPointImplementation {
            KaFirAnnotationListForDeclaration.create(firSymbol)
        }

    override val callableId: CallableId?
        get() = entryPointImplementation { firSymbol.delegateFunctionSymbol.callableId }

    override val parameter: KaValueParameterSymbol
        get() = entryPointImplementation { firSymbol.createKtValueParameters().single() }

    override val hasStableParameterNames: Boolean
        get() = entryPointImplementation { firSymbol.fir.hasStableParameterNames }

    override val isExternal: Boolean
        get() = entryPointImplementation { firSymbol.isEffectivelyExternal(analysisSession.firSession) }

    override fun createPointer(): KaSymbolPointer<KaPropertySetterSymbol> = entryPointImplementation {
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
