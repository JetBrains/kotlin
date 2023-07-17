/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KtFe10DescTypeAliasSymbol(
    override val descriptor: TypeAliasDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KtTypeAliasSymbol(), KtFe10DescSymbol<TypeAliasDescriptor> {
    override val name: Name
        get() = withValidityAssertion { descriptor.name }

    override val typeParameters: List<KtTypeParameterSymbol>
        get() = withValidityAssertion { descriptor.declaredTypeParameters.map { KtFe10DescTypeParameterSymbol(it, analysisContext) } }

    override val visibility: Visibility
        get() = withValidityAssertion { descriptor.ktVisibility }

    override val expandedType: KtType
        get() = withValidityAssertion { descriptor.expandedType.toKtType(analysisContext) }

    override val symbolKind: KtSymbolKind
        get() = withValidityAssertion { descriptor.ktSymbolKind }

    override val classIdIfNonLocal: ClassId?
        get() = withValidityAssertion { descriptor.classId }

    override val isActual: Boolean get() = withValidityAssertion { descriptor.isActual }
    override val isExpect: Boolean get() = withValidityAssertion { descriptor.isExpect }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtTypeAliasSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource<KtTypeAliasSymbol>(this) ?: KtFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}