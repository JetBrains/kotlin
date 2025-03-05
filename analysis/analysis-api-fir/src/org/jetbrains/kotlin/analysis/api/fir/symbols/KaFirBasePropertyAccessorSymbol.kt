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
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
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

/**
 * Provides base implementation for property accessors
 */
internal sealed interface KaFirBasePropertyAccessorSymbol : KaFirKtBasedSymbol<KtPropertyAccessor, FirPropertyAccessorSymbol> {
    val owningKaProperty: KaFirKotlinPropertySymbol<*>

    private val isGetter: Boolean get() = this is KaFirBasePropertyGetterSymbol

    override val lazyFirSymbol: Lazy<FirPropertyAccessorSymbol>
        get() = throw UnsupportedOperationException()

    override val backingPsi: KtPropertyAccessor?
        get() {
            val property = owningKaProperty.backingPsi as? KtProperty ?: return null
            return if (isGetter) {
                property.getter
            } else {
                property.setter
            }
        }

    override val firSymbol: FirPropertyAccessorSymbol
        get() {
            val propertySymbol = owningKaProperty.firSymbol
            return if (isGetter) {
                propertySymbol.getterSymbol
            } else {
                propertySymbol.setterSymbol
            } ?: errorWithAttachment("${if (isGetter) "Getter" else "Setter"} is not found") {
                withFirSymbolEntry("property", propertySymbol)
            }
        }

    override val analysisSession: KaFirSession
        get() = owningKaProperty.analysisSession

    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsi ?: findPsi() }

    override val origin: KaSymbolOrigin
        get() = owningKaProperty.origin

    val isExpectImpl: Boolean
        get() = withValidityAssertion {
            backingPsi?.hasModifier(KtTokens.EXPECT_KEYWORD) == true ||
                    owningKaProperty.backingPsi?.isExpectDeclaration() ?: firSymbol.isExpect
        }

    val isInlineImpl: Boolean
        get() = withValidityAssertion {
            backingPsi?.hasModifier(KtTokens.INLINE_KEYWORD) == true || owningKaProperty.backingPsi?.hasModifier(KtTokens.INLINE_KEYWORD) ?: firSymbol.isInline
        }

    val hasBodyImpl: Boolean
        get() = withValidityAssertion {
            owningKaProperty.ifSource {
                if (backingPsi?.hasBody() == true) {
                    return true
                }

                val property = owningKaProperty.backingPsi
                property is KtProperty && property.hasDelegate()
            } ?: firSymbol.fir.hasBody
        }

    val isDefaultImpl: Boolean
        get() = withValidityAssertion {
            owningKaProperty.ifSource {
                when (val property = owningKaProperty.backingPsi) {
                    // unsure -> compute by fir
                    null -> null

                    is KtProperty -> !property.hasDelegate() && backingPsi == null

                    // only properties may have non-default accessors
                    else -> true
                }
            } ?: (firSymbol.fir is FirDefaultPropertyAccessor)
        }

    val modalityImpl: KaSymbolModality
        get() = withValidityAssertion {
            backingPsi?.kaSymbolModalityByModifiers ?: owningKaProperty.modalityByPsi ?: firSymbol.kaSymbolModality
        }

    val compilerVisibilityImpl: Visibility
        get() = withValidityAssertion {
            backingPsi?.visibilityByModifiers ?: owningKaProperty.compilerVisibilityByPsi ?: firSymbol.visibility
        }

    val returnTypeImpl: KaType
        get() = withValidityAssertion { createReturnType() }

    val receiverParameterImpl: KaReceiverParameterSymbol?
        get() = withValidityAssertion { owningKaProperty.receiverParameter }

    val callableIdImpl: CallableId?
        get() = withValidityAssertion { null }

    val hasStableParameterNamesImpl: Boolean
        get() = withValidityAssertion { true }

    private val annotationUseSiteTarget: AnnotationUseSiteTarget
        get() = if (isGetter) AnnotationUseSiteTarget.PROPERTY_GETTER else AnnotationUseSiteTarget.PROPERTY_SETTER

    val annotationsImpl: KaAnnotationList
        get() = withValidityAssertion {
            if (backingPsi?.annotationEntries.isNullOrEmpty() &&
                owningKaProperty.backingPsi?.hasAnnotation(annotationUseSiteTarget) == false
            ) {
                KaBaseEmptyAnnotationList(token)
            } else {
                KaFirAnnotationListForDeclaration.create(firSymbol, builder)
            }
        }

    val isOverrideImpl: Boolean
}

internal interface KaFirBasePropertyGetterSymbol : KaFirBasePropertyAccessorSymbol {
    override val isOverrideImpl: Boolean
        get() = withValidityAssertion {
            owningKaProperty.ifSource { owningKaProperty.backingPsi }?.hasModifier(KtTokens.OVERRIDE_KEYWORD)
                ?: (firSymbol.isOverride || firSymbol.fir.propertySymbol.isOverride)
        }
}

internal interface KaFirBasePropertySetterSymbol : KaFirBasePropertyAccessorSymbol {
    override val returnTypeImpl: KaType
        get() = withValidityAssertion { analysisSession.builtinTypes.unit }

    override val isOverrideImpl: Boolean
        get() = withValidityAssertion {
            // The existence of the `override` keyword doesn't guarantee that the setter overrides something
            // as its base version may have `val`
            owningKaProperty.isOverride && firSymbol.isSetterOverride(analysisSession)
        }

    val parameterImpl: KaValueParameterSymbol
        get() = withValidityAssertion {
            with(analysisSession) {
                backingPsi?.valueParameters?.firstOrNull()?.symbol as? KaValueParameterSymbol
            } ?: firSymbol.createKtValueParameters(builder).single()
        }
}
