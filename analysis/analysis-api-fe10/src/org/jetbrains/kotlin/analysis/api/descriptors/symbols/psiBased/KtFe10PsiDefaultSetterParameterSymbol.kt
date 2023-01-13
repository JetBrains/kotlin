/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KtFe10Symbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10PsiDefaultSetterParameterSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.createErrorType
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KtEmptyAnnotationsList
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.resolve.BindingContext

internal class KtFe10PsiDefaultSetterParameterSymbol(
    private val accessorPsi: KtPropertyAccessor,
    override val analysisContext: Fe10AnalysisContext
) : KtValueParameterSymbol(), KtFe10Symbol {
    val descriptor: VariableDescriptor? by cached {
        val bindingContext = analysisContext.analyze(accessorPsi, AnalysisMode.PARTIAL)
        bindingContext[BindingContext.PROPERTY_ACCESSOR, accessorPsi]?.valueParameters?.single()
    }

    override val origin: KtSymbolOrigin
        get() = withValidityAssertion { KtSymbolOrigin.SOURCE_MEMBER_GENERATED }

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

    override val returnType: KtType
        get() = withValidityAssertion { descriptor?.type?.toKtType(analysisContext) ?: createErrorType() }

    override val psi: PsiElement?
        get() = withValidityAssertion { null }

    override val name: Name
        get() = withValidityAssertion { descriptor?.name ?: Name.identifier("value") }

    override val annotationsList: KtAnnotationsList
        get() = withValidityAssertion { KtEmptyAnnotationsList(token) }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtValueParameterSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromPsi<KtPropertySetterSymbol>(accessorPsi)
            ?.let(::KtFe10PsiDefaultSetterParameterSymbolPointer)
            ?: KtFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}
