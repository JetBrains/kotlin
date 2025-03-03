/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.*
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaBaseEmptyAnnotationList
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.asKaSymbolModality
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBasePropertySetterSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.psiUtil.isExpectDeclaration
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment

/**
 * Represents the default property setter for [KaFirKotlinPropertyKtParameterBasedSymbol] and [KaFirKotlinPropertyKtDestructuringDeclarationEntryBasedSymbol].
 *
 * Also represents [KaFirKotlinPropertyKtPropertyBasedSymbol] with backing [KtPropertyAccessor] without a body.
 */
internal class KaFirDefaultPropertySetterSymbol(
    val owningKaProperty: KaFirKotlinPropertySymbol<*>,
) : KaPropertySetterSymbol(), KaFirPsiSymbol<KtPropertyAccessor, FirPropertyAccessorSymbol> {
    override val backingPsi: KtPropertyAccessor?
        get() = (owningKaProperty.backingPsi as? KtProperty)?.setter

    init {
        requireWithAttachment(
            backingPsi?.hasBody() != true,
            { "This implementation should not be created for property accessor with a body" },
        ) {
            withFirSymbolEntry("property", owningKaProperty.firSymbol)
        }
    }

    override val analysisSession: KaFirSession
        get() = owningKaProperty.analysisSession

    override val lazyFirSymbol: Lazy<FirPropertyAccessorSymbol>
        get() = throw UnsupportedOperationException()

    override val firSymbol: FirPropertyAccessorSymbol
        get() = owningKaProperty.firSymbol.setterSymbol ?: errorWithAttachment("Setter is not found") {
            withFirSymbolEntry("property", owningKaProperty.firSymbol)
        }

    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsi }

    override val isExpect: Boolean
        get() = withValidityAssertion {
            backingPsi?.hasModifier(KtTokens.EXPECT_KEYWORD) == true ||
                    owningKaProperty.backingPsi?.isExpectDeclaration() ?: firSymbol.isExpect
        }

    override val isDefault: Boolean
        get() = withValidityAssertion { true }

    override val isInline: Boolean
        get() = withValidityAssertion {
            backingPsi?.hasModifier(KtTokens.INLINE_KEYWORD) == true ||
                    owningKaProperty.backingPsi?.hasModifier(KtTokens.INLINE_KEYWORD) ?: firSymbol.isInline
        }

    override val isOverride: Boolean
        get() = withValidityAssertion {
            if (!owningKaProperty.isOverride) {
                return false
            }

            firSymbol.isSetterOverride(analysisSession)
        }

    override val hasBody: Boolean
        get() = withValidityAssertion { false }

    override val modality: KaSymbolModality
        get() = withValidityAssertion {
            backingPsi?.kaSymbolModalityByModifiers
                ?: owningKaProperty.modalityByPsi
                ?: firSymbol.modality.asKaSymbolModality
        }

    override val compilerVisibility: Visibility
        get() = withValidityAssertion {
            backingPsi?.visibilityByModifiers
                ?: owningKaProperty.compilerVisibilityByPsi
                ?: firSymbol.visibility
        }

    override val returnType: KaType
        get() = withValidityAssertion { analysisSession.builtinTypes.unit }

    override val receiverParameter: KaReceiverParameterSymbol?
        get() = withValidityAssertion {
            owningKaProperty.receiverParameter
        }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion {
            val backingPsi = owningKaProperty.backingPsi
            if (backingPsi != null &&
                !backingPsi.hasAnnotation(AnnotationUseSiteTarget.PROPERTY_SETTER) &&
                this.backingPsi?.annotationEntries.isNullOrEmpty()
            )
                KaBaseEmptyAnnotationList(token)
            else
                KaFirAnnotationListForDeclaration.Companion.create(firSymbol, builder)
        }

    override val callableId: CallableId?
        get() = withValidityAssertion { null }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { true }

    override val parameter: KaValueParameterSymbol
        get() = withValidityAssertion {
            with(analysisSession) {
                backingPsi?.valueParameters?.firstOrNull()?.symbol as? KaValueParameterSymbol
            } ?: KaFirDefaultSetterValueParameter(this)
        }

    override val origin: KaSymbolOrigin
        get() = withValidityAssertion { owningKaProperty.origin }

    override fun createPointer(): KaSymbolPointer<KaPropertySetterSymbol> = withValidityAssertion {
        KaBasePropertySetterSymbolPointer(owningKaProperty.createPointer(), this)
    }

    override fun equals(other: Any?): Boolean = this === other ||
            other is KaFirDefaultPropertySetterSymbol &&
            other.owningKaProperty == owningKaProperty

    override fun hashCode(): Int = 31 * owningKaProperty.hashCode() + KaFirKotlinPropertySymbol.HASH_CODE_ADDITION_FOR_SETTER
}
