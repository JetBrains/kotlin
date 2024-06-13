/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktVisibility
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.KaFe10PsiSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.createErrorType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.kaSymbolLocation
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.ktVisibility
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolLocation
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.resolve.BindingContext

internal class KaFe10PsiTypeAliasSymbol(
    override val psi: KtTypeAlias,
    override val analysisContext: Fe10AnalysisContext
) : KaTypeAliasSymbol(), KaFe10PsiSymbol<KtTypeAlias, TypeAliasDescriptor> {
    override val descriptor: TypeAliasDescriptor? by cached {
        val bindingContext = analysisContext.analyze(psi)
        bindingContext[BindingContext.TYPE_ALIAS, psi]
    }

    override val name: Name
        get() = withValidityAssertion { psi.nameAsSafeName }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion { psi.typeParameters.map { KaFe10PsiTypeParameterSymbol(it, analysisContext) } }

    override val visibility: Visibility
        get() = withValidityAssertion { psi.ktVisibility ?: descriptor?.ktVisibility ?: Visibilities.Public }

    override val expandedType: KaType
        get() = withValidityAssertion { descriptor?.expandedType?.toKtType(analysisContext) ?: createErrorType() }

    override val classId: ClassId?
        get() = withValidityAssertion { psi.getClassId() }

    override val location: KaSymbolLocation
        get() = withValidityAssertion { psi.kaSymbolLocation }

    override val isActual: Boolean get() = withValidityAssertion { descriptor?.isActual ?: psi.hasActualModifier() }
    override val isExpect: Boolean get() = withValidityAssertion { descriptor?.isExpect ?: psi.hasExpectModifier() }

    override fun createPointer(): KaSymbolPointer<KaTypeAliasSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaTypeAliasSymbol>(this) ?: KaFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}
