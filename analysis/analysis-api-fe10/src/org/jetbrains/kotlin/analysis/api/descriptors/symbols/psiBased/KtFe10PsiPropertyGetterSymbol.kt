/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktModality
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktVisibility
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.KaFe10PsiSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.createErrorType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.ktModality
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.ktVisibility
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyGetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.resolve.BindingContext

internal class KaFe10PsiPropertyGetterSymbol(
    override val psi: KtPropertyAccessor,
    override val analysisContext: Fe10AnalysisContext
) : KaPropertyGetterSymbol(), KaFe10PsiSymbol<KtPropertyAccessor, PropertyGetterDescriptor> {
    override val descriptor: PropertyGetterDescriptor? by cached {
        val bindingContext = analysisContext.analyze(psi, AnalysisMode.PARTIAL)
        bindingContext[BindingContext.PROPERTY_ACCESSOR, psi] as? PropertyGetterDescriptor
    }

    override val modality: Modality
        get() = withValidityAssertion { psi.property.ktModality ?: descriptor?.ktModality ?: Modality.FINAL }

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

    override val valueParameters: List<KaValueParameterSymbol>
        get() = withValidityAssertion { psi.valueParameters.map { KaFe10PsiValueParameterSymbol(it, analysisContext) } }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { true }

    override val callableId: CallableId?
        get() = withValidityAssertion { null }

    override val returnType: KaType
        get() = withValidityAssertion {
            descriptor?.returnType?.toKtType(analysisContext) ?: createErrorType()
        }
    override val receiverParameter: KaReceiverParameterSymbol?
        get() = withValidityAssertion {
            descriptor?.extensionReceiverParameter?.toKtReceiverParameterSymbol(analysisContext)
        }

    override fun createPointer(): KaSymbolPointer<KaPropertyGetterSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaPropertyGetterSymbol>(this) ?: KaFe10NeverRestoringSymbolPointer()
    }


    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}
