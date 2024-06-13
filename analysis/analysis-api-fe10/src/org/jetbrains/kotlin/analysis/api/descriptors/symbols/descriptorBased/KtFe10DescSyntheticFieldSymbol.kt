/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KaFe10AnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KaFe10Symbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10DescSyntheticFieldSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyAccessorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor
import org.jetbrains.kotlin.load.kotlin.toSourceElement
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.resolve.source.getPsi

internal class KaFe10DescSyntheticFieldSymbol(
    val descriptor: SyntheticFieldDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KaBackingFieldSymbol(), KaFe10Symbol, KaFe10AnnotatedSymbol {
    override val owningProperty: KaKotlinPropertySymbol
        get() = withValidityAssertion {
            val kotlinProperty = descriptor.propertyDescriptor as PropertyDescriptorImpl
            KaFe10DescKotlinPropertySymbol(kotlinProperty, analysisContext)
        }

    override val annotationsObject: Annotations
        get() = withValidityAssertion { descriptor.annotations }

    override val returnType: KaType
        get() = withValidityAssertion { descriptor.propertyDescriptor.type.toKtType(analysisContext) }

    override fun createPointer(): KaSymbolPointer<KaBackingFieldSymbol> = withValidityAssertion {
        val accessorPsi = descriptor.containingDeclaration.toSourceElement.getPsi()
        if (accessorPsi is KtPropertyAccessor) {
            val accessorPointer = KaPsiBasedSymbolPointer.createForSymbolFromPsi<KaPropertyAccessorSymbol>(accessorPsi)
            if (accessorPointer != null) {
                return KaFe10DescSyntheticFieldSymbolPointer(accessorPointer)
            }
        }

        return KaFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}