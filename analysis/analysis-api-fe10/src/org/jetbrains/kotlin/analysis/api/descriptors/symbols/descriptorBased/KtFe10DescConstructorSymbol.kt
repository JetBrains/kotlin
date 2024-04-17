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
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10DescFunctionLikeSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.name.ClassId

internal class KtFe10DescConstructorSymbol(
    override val descriptor: ConstructorDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KtConstructorSymbol(), KtFe10DescMemberSymbol<ConstructorDescriptor> {
    override val isPrimary: Boolean
        get() = withValidityAssertion { descriptor.isPrimary }

    override val containingClassIdIfNonLocal: ClassId?
        get() = withValidityAssertion { descriptor.constructedClass.classId }

    override val valueParameters: List<KtValueParameterSymbol>
        get() = withValidityAssertion { descriptor.valueParameters.map { KtFe10DescValueParameterSymbol(it, analysisContext) } }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { descriptor.ktHasStableParameterNames }

    override val returnType: KtType
        get() = withValidityAssertion { descriptor.returnType.toKtType(analysisContext) }

    override val isActual: Boolean
        get() = withValidityAssertion { descriptor.isActual }

    override val isExpect: Boolean
        get() = withValidityAssertion { descriptor.isExpect }

    override val typeParameters: List<KtTypeParameterSymbol>
        get() = withValidityAssertion {
            descriptor.typeParameters.mapNotNull {
                if (it.containingDeclaration != descriptor) return@mapNotNull null
                KtFe10DescTypeParameterSymbol(it, analysisContext)
            }
        }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtConstructorSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource<KtConstructorSymbol>(this)?.let {
            return it
        }

        val callableId = descriptor.callableIdIfNotLocal
        if (callableId != null) {
            val signature = descriptor.getSymbolPointerSignature()
            return KtFe10DescFunctionLikeSymbolPointer(callableId, signature)
        }

        return KtFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}