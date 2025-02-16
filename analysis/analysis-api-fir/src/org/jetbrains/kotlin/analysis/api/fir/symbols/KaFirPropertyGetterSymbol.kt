/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.*
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaBaseEmptyAnnotationList
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBasePropertyGetterSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyGetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.psiUtil.isExpectDeclaration
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class KaFirPropertyGetterSymbol(
    val owningKaProperty: KaFirKotlinPropertySymbol<KtProperty>,
) : KaPropertyGetterSymbol(), KaFirKtBasedSymbol<KtPropertyAccessor, FirPropertyAccessorSymbol> {
    override val backingPsi: KtPropertyAccessor? = owningKaProperty.backingPsi?.getter

    override val analysisSession: KaFirSession
        get() = owningKaProperty.analysisSession

    override val lazyFirSymbol: Lazy<FirPropertyAccessorSymbol>
        get() = throw UnsupportedOperationException()

    override val firSymbol: FirPropertyAccessorSymbol
        get() = owningKaProperty.firSymbol.getterSymbol ?: errorWithAttachment("Getter is not found") {
            withFirSymbolEntry("property", owningKaProperty.firSymbol)
        }

    init {
        requireWithAttachment(
            backingPsi?.property?.hasRegularGetter != false,
            { "Property getter without a body" },
        ) {
            withPsiEntry("propertyGetter", backingPsi)
            withFirSymbolEntry("firSymbol", firSymbol)
        }
    }

    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsi ?: findPsi() }

    override val isExpect: Boolean
        get() = withValidityAssertion {
            backingPsi?.hasModifier(KtTokens.EXPECT_KEYWORD) == true ||
                    owningKaProperty.backingPsi?.isExpectDeclaration() ?: firSymbol.isExpect
        }

    override val isDefault: Boolean
        get() = withValidityAssertion {
            if (ifSource { backingPsi } != null)
                false
            else
                firSymbol.fir is FirDefaultPropertyAccessor
        }

    override val isInline: Boolean
        get() = withValidityAssertion {
            owningKaProperty.backingPsi?.hasModifier(KtTokens.INLINE_KEYWORD) ?: firSymbol.isInline
        }

    override val isOverride: Boolean
        get() = withValidityAssertion {
            ifSource { owningKaProperty.backingPsi }?.hasModifier(KtTokens.OVERRIDE_KEYWORD)
                ?: (firSymbol.isOverride || firSymbol.fir.propertySymbol.isOverride)
        }

    override val hasBody: Boolean
        get() = withValidityAssertion {
            ifSource {
                backingPsi?.hasBody() == true || backingPsi?.property?.hasDelegate() == true
            } ?: firSymbol.fir.hasBody
        }

    override val modality: KaSymbolModality
        get() = withValidityAssertion {
            if (backingPsi != null)
                backingPsi.kaSymbolModalityByModifiers ?: owningKaProperty.modality
            else
                firSymbol.kaSymbolModality
        }

    override val compilerVisibility: Visibility
        get() = withValidityAssertion {
            if (backingPsi != null)
                backingPsi.visibilityByModifiers ?: owningKaProperty.compilerVisibility
            else
                firSymbol.visibility
        }

    override val returnType: KaType
        get() = withValidityAssertion { createReturnType() }

    override val receiverParameter: KaReceiverParameterSymbol?
        get() = withValidityAssertion {
            owningKaProperty.receiverParameter
        }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion {
            if (backingPsi?.annotationEntries.isNullOrEmpty() &&
                owningKaProperty.backingPsi?.hasAnnotation(AnnotationUseSiteTarget.PROPERTY_GETTER) == false
            )
                KaBaseEmptyAnnotationList(token)
            else
                KaFirAnnotationListForDeclaration.create(firSymbol, builder)
        }

    override val callableId: CallableId?
        get() = withValidityAssertion { null }

    override fun createPointer(): KaSymbolPointer<KaPropertyGetterSymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<KaPropertyGetterSymbol>()
            ?: KaBasePropertyGetterSymbolPointer(propertySymbolPointer = owningKaProperty.createPointer(), originalSymbol = this)
    }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()

    companion object {
        fun create(declaration: KtPropertyAccessor, session: KaFirSession): KaPropertyGetterSymbol {
            val property = declaration.property
            val owningKaProperty = with(session) {
                @Suppress("UNCHECKED_CAST")
                property.symbol as KaFirKotlinPropertySymbol<KtProperty>
            }

            return if (property.hasRegularGetter) {
                KaFirPropertyGetterSymbol(owningKaProperty)
            } else {
                KaFirDefaultPropertyGetterSymbol(owningKaProperty)
            }
        }
    }
}
