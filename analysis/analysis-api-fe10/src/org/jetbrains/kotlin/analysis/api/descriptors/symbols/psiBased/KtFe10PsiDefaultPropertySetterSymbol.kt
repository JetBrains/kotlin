/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade
import org.jetbrains.kotlin.analysis.api.descriptors.annotations.KtFe10AnnotationsList
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KtFe10Symbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktModality
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktVisibility
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.createErrorType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.ktModality
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.ktSymbolOrigin
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.ktVisibility
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KtEmptyAnnotationsList
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KtPropertyAccessorSymbolPointer
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KtValueParameterFromDefaultSetterSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.resolve.BindingContext

internal class KtFe10PsiDefaultPropertySetterSymbol(
    private val propertyPsi: KtProperty,
    override val analysisContext: Fe10AnalysisContext
) : KtPropertySetterSymbol(), KtFe10Symbol {
    val descriptor: PropertySetterDescriptor? by cached {
        val bindingContext = analysisContext.analyze(propertyPsi, Fe10AnalysisFacade.AnalysisMode.PARTIAL)
        (bindingContext[BindingContext.VARIABLE, propertyPsi] as? PropertyDescriptor)?.setter
    }

    override val origin: KtSymbolOrigin
        get() = withValidityAssertion { propertyPsi.ktSymbolOrigin }

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

    override val parameter: KtValueParameterSymbol by cached {
        DefaultKtValueParameterSymbol(propertyPsi, descriptor?.valueParameters?.firstOrNull(), analysisContext)
    }

    override val valueParameters: List<KtValueParameterSymbol>
        get() = withValidityAssertion { listOf(parameter) }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { true }

    override val callableIdIfNonLocal: CallableId?
        get() = withValidityAssertion { null }

    override val returnType: KtType
        get() = withValidityAssertion {
            return analysisContext.builtIns.unitType.toKtType(analysisContext)
        }

    override val receiverParameter: KtReceiverParameterSymbol?
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

    override val annotationsList: KtAnnotationsList
        get() = withValidityAssertion {
            descriptor?.let { KtFe10AnnotationsList.create(it.annotations, analysisContext) } ?: KtEmptyAnnotationsList(token)
        }

    context(KtAnalysisSession)
    @OptIn(KtAnalysisApiInternals::class)
    override fun createPointer(): KtSymbolPointer<KtPropertySetterSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromPsi<KtPropertySymbol>(propertyPsi)?.let {
            @Suppress("UNCHECKED_CAST")
            KtPropertyAccessorSymbolPointer(it, isGetter = false) as KtSymbolPointer<KtPropertySetterSymbol>
        } ?: KtFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()

    class DefaultKtValueParameterSymbol(
        private val propertyPsi: KtProperty,
        val descriptor: ValueParameterDescriptor?,
        override val analysisContext: Fe10AnalysisContext
    ) : KtValueParameterSymbol(), KtFe10Symbol {
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

        override val returnType: KtType
            get() = withValidityAssertion { descriptor?.returnType?.toKtType(analysisContext) ?: createErrorType() }

        override val origin: KtSymbolOrigin
            get() = withValidityAssertion { propertyPsi.ktSymbolOrigin }

        override val psi: PsiElement?
            get() = withValidityAssertion { null }

        override val annotationsList: KtAnnotationsList
            get() = withValidityAssertion {
                descriptor?.let { KtFe10AnnotationsList.create(it.annotations, analysisContext) } ?: KtEmptyAnnotationsList(token)
            }

        context(KtAnalysisSession)
        @OptIn(KtAnalysisApiInternals::class)
        override fun createPointer(): KtSymbolPointer<KtValueParameterSymbol> = withValidityAssertion {
            KtPsiBasedSymbolPointer.createForSymbolFromPsi<KtPropertySymbol>(propertyPsi)?.let {
                KtValueParameterFromDefaultSetterSymbolPointer(it)
            } ?: KtFe10NeverRestoringSymbolPointer()
        }

        override fun equals(other: Any?): Boolean = isEqualTo(other)
        override fun hashCode(): Int = calculateHashCode()
    }
}
