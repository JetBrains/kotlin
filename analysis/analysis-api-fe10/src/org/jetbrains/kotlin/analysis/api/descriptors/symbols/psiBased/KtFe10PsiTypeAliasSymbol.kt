/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktVisibility
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.KtFe10PsiSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.createErrorType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.ktSymbolKind
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.ktVisibility
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.resolve.BindingContext

internal class KtFe10PsiTypeAliasSymbol(
    override val psi: KtTypeAlias,
    override val analysisContext: Fe10AnalysisContext
) : KtTypeAliasSymbol(), KtFe10PsiSymbol<KtTypeAlias, TypeAliasDescriptor> {
    override val descriptor: TypeAliasDescriptor? by cached {
        val bindingContext = analysisContext.analyze(psi)
        bindingContext[BindingContext.TYPE_ALIAS, psi]
    }

    override val name: Name
        get() = withValidityAssertion { psi.nameAsSafeName }

    override val typeParameters: List<KtTypeParameterSymbol>
        get() = withValidityAssertion { psi.typeParameters.map { KtFe10PsiTypeParameterSymbol(it, analysisContext) } }

    override val visibility: Visibility
        get() = withValidityAssertion { psi.ktVisibility ?: descriptor?.ktVisibility ?: Visibilities.Public }

    override val expandedType: KtType
        get() = withValidityAssertion { descriptor?.expandedType?.toKtType(analysisContext) ?: createErrorType() }

    override val classIdIfNonLocal: ClassId?
        get() = withValidityAssertion { psi.getClassId() }

    override val symbolKind: KtSymbolKind
        get() = withValidityAssertion { psi.ktSymbolKind }

    override val isActual: Boolean get() = withValidityAssertion { descriptor?.isActual ?: psi.hasActualModifier() }
    override val isExpect: Boolean get() = withValidityAssertion { descriptor?.isExpect ?: psi.hasExpectModifier() }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtTypeAliasSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource<KtTypeAliasSymbol>(this) ?: KtFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}
