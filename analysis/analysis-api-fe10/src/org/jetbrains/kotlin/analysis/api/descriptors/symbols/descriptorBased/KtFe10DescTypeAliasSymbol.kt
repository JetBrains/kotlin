/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KaFe10DescTypeAliasSymbol(
    override val descriptor: TypeAliasDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KaTypeAliasSymbol(), KaFe10DescSymbol<TypeAliasDescriptor> {
    override val name: Name
        get() = withValidityAssertion { descriptor.name }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion { descriptor.declaredTypeParameters.map { KaFe10DescTypeParameterSymbol(it, analysisContext) } }

    override val visibility: Visibility
        get() = withValidityAssertion { descriptor.ktVisibility }

    override val expandedType: KaType
        get() = withValidityAssertion { descriptor.expandedType.toKtType(analysisContext) }

    override val symbolKind: KaSymbolKind
        get() = withValidityAssertion { descriptor.ktSymbolKind }

    override val classId: ClassId?
        get() = withValidityAssertion { descriptor.classId }

    override val isActual: Boolean get() = withValidityAssertion { descriptor.isActual }
    override val isExpect: Boolean get() = withValidityAssertion { descriptor.isExpect }

    override fun createPointer(): KaSymbolPointer<KaTypeAliasSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaTypeAliasSymbol>(this) ?: KaFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}