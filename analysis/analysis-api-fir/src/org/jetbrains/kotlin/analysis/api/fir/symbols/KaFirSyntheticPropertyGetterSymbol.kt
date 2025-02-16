/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createOwnerPointer
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBasePropertyGetterSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyGetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.utils.hasBody
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.isExtension
import org.jetbrains.kotlin.name.CallableId

internal class KaFirSyntheticPropertyGetterSymbol(
    override val firSymbol: FirSyntheticPropertyAccessorSymbol,
    override val analysisSession: KaFirSession,
) : KaPropertyGetterSymbol(), KaFirSymbol<FirSyntheticPropertyAccessorSymbol> {
    init {
        require(firSymbol.isGetter)
    }

    override val psi: PsiElement?
        get() = withValidityAssertion { findPsi() }

    override val isDefault: Boolean
        get() = withValidityAssertion { false }

    override val isInline: Boolean
        get() = withValidityAssertion { false }

    override val isOverride: Boolean
        get() = withValidityAssertion {
            firSymbol.isOverride || firSymbol.fir.propertySymbol.isOverride
        }

    override val isExtension: Boolean
        get() = withValidityAssertion { firSymbol.isExtension }

    override val hasBody: Boolean
        get() = withValidityAssertion { firSymbol.fir.hasBody }

    override val modality: KaSymbolModality
        get() = withValidityAssertion { firSymbol.kaSymbolModality }

    override val compilerVisibility: Visibility
        get() = withValidityAssertion { firSymbol.visibility }

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

    override fun createPointer(): KaSymbolPointer<KaPropertyGetterSymbol> = withValidityAssertion {
        KaBasePropertyGetterSymbolPointer(propertySymbolPointer = analysisSession.createOwnerPointer(this), originalSymbol = this)
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}