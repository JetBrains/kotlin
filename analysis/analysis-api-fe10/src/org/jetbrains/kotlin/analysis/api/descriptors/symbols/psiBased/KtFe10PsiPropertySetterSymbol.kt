/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktVisibility
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtTypeAndAnnotations
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtTypeAndAnnotations
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.resolve.BindingContext

internal class KtFe10PsiPropertySetterSymbol(
    override val psi: KtPropertyAccessor,
    override val analysisSession: KtFe10AnalysisSession
) : KtPropertySetterSymbol(), KtFe10PsiSymbol<KtPropertyAccessor, PropertySetterDescriptor> {
    override val descriptor: PropertySetterDescriptor? by cached {
        val bindingContext = analysisSession.analyze(psi, KtFe10AnalysisSession.AnalysisMode.PARTIAL)
        bindingContext[BindingContext.PROPERTY_ACCESSOR, psi] as? PropertySetterDescriptor
    }

    override val modality: Modality
        get() = withValidityAssertion { psi.property.ktModality ?: descriptor?.modality ?: Modality.FINAL }

    override val visibility: Visibility
        get() = withValidityAssertion { psi.ktVisibility ?: descriptor?.ktVisibility ?: psi.property.ktVisibility ?: Visibilities.Public }

    override val isDefault: Boolean
        get() = withValidityAssertion { false }

    override val isInline: Boolean
        get() = withValidityAssertion { psi.hasModifier(KtTokens.INLINE_KEYWORD) || psi.property.hasModifier(KtTokens.INLINE_KEYWORD) }

    override val isOverride: Boolean
        get() = withValidityAssertion { psi.property.hasModifier(KtTokens.OVERRIDE_KEYWORD) }

    override val hasBody: Boolean
        get() = withValidityAssertion { psi.hasBody() }

    override val valueParameters: List<KtValueParameterSymbol>
        get() = withValidityAssertion { psi.valueParameters.map { KtFe10PsiValueParameterSymbol(it, analysisSession) } }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { true }

    override val callableIdIfNonLocal: CallableId?
        get() = TODO("Not yet implemented")

    override val annotatedType: KtTypeAndAnnotations
        get() = withValidityAssertion {
            descriptor?.returnType?.toKtTypeAndAnnotations(analysisSession) ?: createErrorTypeAndAnnotations()
        }
    override val receiverType: KtTypeAndAnnotations?
        get() = withValidityAssertion {
            val descriptor = this.descriptor
            return when {
                descriptor != null -> descriptor.extensionReceiverParameter?.type?.toKtTypeAndAnnotations(analysisSession)
                psi.property.isExtensionDeclaration() -> createErrorTypeAndAnnotations()
                else -> null
            }
        }

    override val dispatchType: KtType?
        get() = withValidityAssertion {
            val descriptor = this.descriptor
            return when {
                descriptor != null -> descriptor.dispatchReceiverParameter?.type?.toKtType(analysisSession)
                !psi.property.isTopLevel -> createErrorType()
                else -> null
            }
        }

    override val parameter: KtValueParameterSymbol
        get() = withValidityAssertion {
            val parameter = psi.parameter
            return if (parameter != null) {
                KtFe10PsiValueParameterSymbol(parameter, analysisSession)
            } else {
                KtFe10PsiDefaultSetterParameterSymbol(psi, analysisSession)
            }
        }

    override fun createPointer(): KtSymbolPointer<KtPropertySetterSymbol> = withValidityAssertion {
        return KtPsiBasedSymbolPointer.createForSymbolFromSource(this) ?: KtFe10NeverRestoringSymbolPointer()
    }
}