/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationsList
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade
import org.jetbrains.kotlin.analysis.api.descriptors.annotations.KaFe10AnnotationsList
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KaFe10Symbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktModality
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktVisibility
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.createErrorType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.ktModality
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.kaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.ktVisibility
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaEmptyAnnotationsList
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaPropertySetterSymbolPointer
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaValueParameterFromDefaultSetterSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.resolve.BindingContext

internal class KaFe10PsiDefaultPropertySetterSymbol(
    private val propertyPsi: KtProperty,
    override val analysisContext: Fe10AnalysisContext
) : KaPropertySetterSymbol(), KaFe10Symbol {
    val descriptor: PropertySetterDescriptor? by cached {
        val bindingContext = analysisContext.analyze(propertyPsi, Fe10AnalysisFacade.AnalysisMode.PARTIAL)
        (bindingContext[BindingContext.VARIABLE, propertyPsi] as? PropertyDescriptor)?.setter
    }

    override val origin: KaSymbolOrigin
        get() = withValidityAssertion { propertyPsi.kaSymbolOrigin }

    override val psi: PsiElement?
        get() = withValidityAssertion { null }

    override val isDefault: Boolean
        get() = withValidityAssertion { true }

    override val isInline: Boolean
        get() = withValidityAssertion { propertyPsi.hasModifier(KtTokens.OVERRIDE_KEYWORD) }

    override val isOverride: Boolean
        get() = withValidityAssertion { propertyPsi.hasModifier(KtTokens.OVERRIDE_KEYWORD) }

    override val hasBody: Boolean
        get() = withValidityAssertion { false }

    override val parameter: KaValueParameterSymbol by cached {
        KaDefaultValueParameterSymbol(propertyPsi, descriptor?.valueParameters?.firstOrNull(), analysisContext)
    }

    override val valueParameters: List<KaValueParameterSymbol>
        get() = withValidityAssertion { listOf(parameter) }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { true }

    override val callableId: CallableId?
        get() = withValidityAssertion { null }

    override val returnType: KaType
        get() = withValidityAssertion {
            return analysisContext.builtIns.unitType.toKtType(analysisContext)
        }

    override val receiverParameter: KaReceiverParameterSymbol?
        get() = withValidityAssertion {
            if (!propertyPsi.isExtensionDeclaration()) {
                return null
            }

            descriptor?.extensionReceiverParameter?.toKtReceiverParameterSymbol(analysisContext)
        }

    override val modality: Modality
        get() = withValidityAssertion { propertyPsi.ktModality ?: descriptor?.ktModality ?: Modality.FINAL }

    override val visibility: Visibility
        get() = withValidityAssertion { propertyPsi.ktVisibility ?: descriptor?.ktVisibility ?: Visibilities.Public }

    override val annotationsList: KaAnnotationsList
        get() = withValidityAssertion {
            descriptor?.let { KaFe10AnnotationsList.create(it.annotations, analysisContext) } ?: KaEmptyAnnotationsList(token)
        }

    @OptIn(KaAnalysisApiInternals::class)
    override fun createPointer(): KaSymbolPointer<KaPropertySetterSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromPsi<KaPropertySymbol>(propertyPsi)?.let(::KaPropertySetterSymbolPointer)
            ?: KaFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()

    class KaDefaultValueParameterSymbol(
        private val propertyPsi: KtProperty,
        val descriptor: ValueParameterDescriptor?,
        override val analysisContext: Fe10AnalysisContext
    ) : KaValueParameterSymbol(), KaFe10Symbol {
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
            get() = withValidityAssertion { descriptor?.returnType?.toKtType(analysisContext) ?: createErrorType() }

        override val origin: KaSymbolOrigin
            get() = withValidityAssertion { propertyPsi.kaSymbolOrigin }

        override val psi: PsiElement?
            get() = withValidityAssertion { null }

        override val annotationsList: KaAnnotationsList
            get() = withValidityAssertion {
                descriptor?.let { KaFe10AnnotationsList.create(it.annotations, analysisContext) } ?: KaEmptyAnnotationsList(token)
            }

        @OptIn(KaAnalysisApiInternals::class)
        override fun createPointer(): KaSymbolPointer<KaValueParameterSymbol> = withValidityAssertion {
            KaPsiBasedSymbolPointer.createForSymbolFromPsi<KaPropertySymbol>(propertyPsi)?.let {
                KaValueParameterFromDefaultSetterSymbolPointer(it)
            } ?: KaFe10NeverRestoringSymbolPointer()
        }

        override fun equals(other: Any?): Boolean = isEqualTo(other)
        override fun hashCode(): Int = calculateHashCode()
    }
}
