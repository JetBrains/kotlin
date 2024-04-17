/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KtFe10AnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KtFe10Symbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10DescSyntheticFieldSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertyAccessorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtVariableLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor
import org.jetbrains.kotlin.load.kotlin.toSourceElement
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.resolve.source.getPsi

internal class KtFe10DescSyntheticFieldSymbol(
    val descriptor: SyntheticFieldDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KtBackingFieldSymbol(), KtFe10Symbol, KtFe10AnnotatedSymbol {
    override val owningProperty: KtKotlinPropertySymbol
        get() = withValidityAssertion {
            val kotlinProperty = descriptor.propertyDescriptor as PropertyDescriptorImpl
            KtFe10DescKotlinPropertySymbol(kotlinProperty, analysisContext)
        }

    override val annotationsObject: Annotations
        get() = withValidityAssertion { descriptor.annotations }

    override val returnType: KtType
        get() = withValidityAssertion { descriptor.propertyDescriptor.type.toKtType(analysisContext) }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtBackingFieldSymbol> = withValidityAssertion {
        val accessorPsi = descriptor.containingDeclaration.toSourceElement.getPsi()
        if (accessorPsi is KtPropertyAccessor) {
            val accessorPointer = KtPsiBasedSymbolPointer.createForSymbolFromPsi<KtPropertyAccessorSymbol>(accessorPsi)
            if (accessorPointer != null) {
                return KtFe10DescSyntheticFieldSymbolPointer(accessorPointer)
            }
        }

        return KtFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}