/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktVisibility
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.KtFe10PsiSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.createErrorType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.ktVisibility
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.resolve.BindingContext

internal class KtFe10PsiConstructorSymbol(
    override val psi: KtConstructor<*>,
    override val analysisContext: Fe10AnalysisContext
) : KtConstructorSymbol(), KtFe10PsiSymbol<KtConstructor<*>, ConstructorDescriptor> {
    override val descriptor: ConstructorDescriptor? by cached {
        val bindingContext = analysisContext.analyze(psi, AnalysisMode.PARTIAL)
        bindingContext[BindingContext.CONSTRUCTOR, psi]
    }

    override val isPrimary: Boolean
        get() = withValidityAssertion { psi is KtPrimaryConstructor }

    override val containingClassIdIfNonLocal: ClassId?
        get() = withValidityAssertion { psi.getContainingClassOrObject().getClassId() }

    override val valueParameters: List<KtValueParameterSymbol>
        get() = withValidityAssertion { psi.valueParameters.map { KtFe10PsiValueParameterSymbol(it, analysisContext) } }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { true }

    override val returnType: KtType
        get() = withValidityAssertion {
            descriptor?.returnType?.toKtType(analysisContext) ?: createErrorType()
        }

    override val visibility: Visibility
        get() = withValidityAssertion { psi.ktVisibility ?: descriptor?.ktVisibility ?: Visibilities.Public }

    override val isActual: Boolean
        get() = withValidityAssertion { descriptor?.isActual ?: psi.hasActualModifier() }

    override val isExpect: Boolean
        get() = withValidityAssertion { descriptor?.isExpect ?: psi.hasExpectModifier() }

    override val typeParameters: List<KtTypeParameterSymbol>
        get() = withValidityAssertion { psi.typeParameters.map { KtFe10PsiTypeParameterSymbol(it, analysisContext) } }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtConstructorSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource<KtConstructorSymbol>(this) ?: KtFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}
