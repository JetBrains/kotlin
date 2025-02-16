/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KaFe10Symbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktVisibility
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10PsiDefaultSetterParameterSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.createErrorType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.ktVisibility
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaBaseEmptyAnnotationList
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBasePsiSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.resolve.BindingContext

internal class KaFe10PsiDefaultSetterParameterSymbol(
    private val accessorPsi: KtPropertyAccessor,
    override val analysisContext: Fe10AnalysisContext
) : KaValueParameterSymbol(), KaFe10Symbol {
    val descriptor: VariableDescriptor? by cached {
        val bindingContext = analysisContext.analyze(accessorPsi, AnalysisMode.PARTIAL)
        bindingContext[BindingContext.PROPERTY_ACCESSOR, accessorPsi]?.valueParameters?.single()
    }

    override val origin: KaSymbolOrigin
        get() = withValidityAssertion { KaSymbolOrigin.SOURCE_MEMBER_GENERATED }

    override val hasDefaultValue: Boolean
        get() = withValidityAssertion { false }

    override val isVararg: Boolean
        get() = withValidityAssertion { false }

    override val isCrossinline: Boolean
        get() = withValidityAssertion { false }

    override val isNoinline: Boolean
        get() = withValidityAssertion { false }

    override val isImplicitLambdaParameter: Boolean
        get() = withValidityAssertion { false }

    override val returnType: KaType
        get() = withValidityAssertion { descriptor?.type?.toKtType(analysisContext) ?: createErrorType() }

    override val compilerVisibility: Visibility
        get() = withValidityAssertion { accessorPsi.ktVisibility ?: descriptor?.ktVisibility ?: Visibilities.Public }

    override val psi: PsiElement?
        get() = withValidityAssertion { null }

    override val name: Name
        get() = withValidityAssertion { descriptor?.name ?: Name.identifier("value") }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion { KaBaseEmptyAnnotationList(token) }

    override fun createPointer(): KaSymbolPointer<KaValueParameterSymbol> = withValidityAssertion {
        KaBasePsiSymbolPointer.createForSymbolFromPsi<KaPropertySetterSymbol>(accessorPsi)
            ?.let {
                KaFe10PsiDefaultSetterParameterSymbolPointer(it, this)
            } ?: KaFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}
