/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10DescFunctionLikeSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.name.ClassId

internal class KaFe10DescConstructorSymbol(
    override val descriptor: ConstructorDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KaConstructorSymbol(), KaFe10DescMemberSymbol<ConstructorDescriptor> {
    override val isPrimary: Boolean
        get() = withValidityAssertion { descriptor.isPrimary }

    override val containingClassId: ClassId?
        get() = withValidityAssertion { descriptor.constructedClass.classId }

    override val valueParameters: List<KaValueParameterSymbol>
        get() = withValidityAssertion { descriptor.valueParameters.map { KaFe10DescValueParameterSymbol(it, analysisContext) } }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { descriptor.ktHasStableParameterNames }

    override val returnType: KaType
        get() = withValidityAssertion { descriptor.returnType.toKtType(analysisContext) }

    override val isActual: Boolean
        get() = withValidityAssertion { descriptor.isActual }

    override val isExpect: Boolean
        get() = withValidityAssertion { descriptor.isExpect }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion {
            descriptor.typeParameters.mapNotNull {
                if (it.containingDeclaration != descriptor) return@mapNotNull null
                KaFe10DescTypeParameterSymbol(it, analysisContext)
            }
        }

    override fun createPointer(): KaSymbolPointer<KaConstructorSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaConstructorSymbol>(this)?.let {
            return it
        }

        val callableId = descriptor.callableIdIfNotLocal
        if (callableId != null) {
            val signature = descriptor.getSymbolPointerSignature()
            return KaFe10DescFunctionLikeSymbolPointer(callableId, signature)
        }

        return KaFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}