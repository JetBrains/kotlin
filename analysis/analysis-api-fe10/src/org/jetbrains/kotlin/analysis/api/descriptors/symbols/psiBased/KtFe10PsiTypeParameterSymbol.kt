/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.KtFe10PsiSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.Variance

internal class KtFe10PsiTypeParameterSymbol(
    override val psi: KtTypeParameter,
    override val analysisContext: Fe10AnalysisContext
) : KtTypeParameterSymbol(), KtFe10PsiSymbol<KtTypeParameter, TypeParameterDescriptor> {
    override val descriptor: TypeParameterDescriptor? by cached {
        val bindingContext = analysisContext.analyze(psi)
        bindingContext[BindingContext.TYPE_PARAMETER, psi]
    }

    override val variance: Variance
        get() = withValidityAssertion { psi.variance }

    override val upperBounds: List<KtType>
        get() = withValidityAssertion { descriptor?.upperBounds?.map { it.toKtType(analysisContext) } ?: emptyList() }

    override val isReified: Boolean
        get() = withValidityAssertion { psi.hasModifier(KtTokens.REIFIED_KEYWORD) }

    override val name: Name
        get() = withValidityAssertion { psi.nameAsSafeName }

    override fun createPointer(): KtSymbolPointer<KtTypeParameterSymbol> = withValidityAssertion {
        return KtPsiBasedSymbolPointer.createForSymbolFromSource(this) ?: KtFe10NeverRestoringSymbolPointer()
    }
}