/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KaFe10Symbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaEmptyAnnotationList
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

internal class KaFe10DescDefaultPropertySetterSymbol(
    private val propertyDescriptor: PropertyDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KaPropertySetterSymbol(), KaFe10Symbol {
    override val parameter: KaValueParameterSymbol by cached {
        KaDefaultValueParameterSymbol(propertyDescriptor, analysisContext)
    }

    override val isDefault: Boolean
        get() = withValidityAssertion { true }

    override val isInline: Boolean
        get() = withValidityAssertion { false }

    override val isOverride: Boolean
        get() = withValidityAssertion { propertyDescriptor.isExplicitOverride }

    override val hasBody: Boolean
        get() = withValidityAssertion { false }

    override val valueParameters: List<KaValueParameterSymbol>
        get() = withValidityAssertion { listOf(parameter) }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { true }

    override val callableId: CallableId?
        get() = withValidityAssertion { propertyDescriptor.setterCallableIdIfNotLocal }

    override val returnType: KaType
        get() = withValidityAssertion { analysisContext.builtIns.unitType.toKtType(analysisContext) }

    override val origin: KaSymbolOrigin
        get() = withValidityAssertion { propertyDescriptor.getSymbolOrigin(analysisContext) }

    override val psi: PsiElement?
        get() = withValidityAssertion { null }

    override val receiverParameter: KaReceiverParameterSymbol?
        get() = withValidityAssertion { propertyDescriptor.extensionReceiverParameter?.toKtReceiverParameterSymbol(analysisContext) }

    override val modality: Modality
        get() = withValidityAssertion { propertyDescriptor.ktModality }

    override val visibility: Visibility
        get() = withValidityAssertion { propertyDescriptor.ktVisibility }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion { KaEmptyAnnotationList(token) }

    override fun createPointer(): KaSymbolPointer<KaPropertySetterSymbol> = withValidityAssertion {
        KaFe10NeverRestoringSymbolPointer()
    }

    class KaDefaultValueParameterSymbol(
        private val propertyDescriptor: PropertyDescriptor,
        override val analysisContext: Fe10AnalysisContext
    ) : KaValueParameterSymbol(), KaFe10Symbol {
        val descriptor: ValueParameterDescriptor?
            get() = propertyDescriptor.setter?.valueParameters?.singleOrNull()

        override val hasDefaultValue: Boolean
            get() = withValidityAssertion { false }

        override val isVararg: Boolean
            get() = withValidityAssertion { false }

        override val isImplicitLambdaParameter: Boolean
            get() = withValidityAssertion { false }

        override val isCrossinline: Boolean
            get() = withValidityAssertion { false }

        override val isNoinline: Boolean
            get() = withValidityAssertion { false }

        override val name: Name
            get() = withValidityAssertion { Name.identifier("value") }

        override val returnType: KaType
            get() = withValidityAssertion { propertyDescriptor.type.toKtType(analysisContext) }

        override val origin: KaSymbolOrigin
            get() = withValidityAssertion { propertyDescriptor.getSymbolOrigin(analysisContext) }

        override val psi: PsiElement?
            get() = withValidityAssertion { null }

        override val annotations: KaAnnotationList
            get() = withValidityAssertion { KaEmptyAnnotationList(token) }

        override fun createPointer(): KaSymbolPointer<KaValueParameterSymbol> = withValidityAssertion {
            KaFe10NeverRestoringSymbolPointer()
        }
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}
