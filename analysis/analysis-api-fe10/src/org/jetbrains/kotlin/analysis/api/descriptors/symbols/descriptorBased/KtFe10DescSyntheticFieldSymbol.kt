/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KtFe10Symbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10DescSyntheticFieldSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.KtBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertyAccessorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtVariableLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor
import org.jetbrains.kotlin.load.kotlin.toSourceElement
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.resolve.source.getPsi

internal class KtFe10DescSyntheticFieldSymbol(
    private val descriptor: SyntheticFieldDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KtBackingFieldSymbol(), KtFe10Symbol {
    override val owningProperty: KtKotlinPropertySymbol
        get() = withValidityAssertion {
            val kotlinProperty = descriptor.propertyDescriptor as PropertyDescriptorImpl
            KtFe10DescKotlinPropertySymbol(kotlinProperty, analysisContext)
        }

    override val type: KtType
        get() = withValidityAssertion { descriptor.propertyDescriptor.type.toKtType(analysisContext) }

    override fun createPointer(): KtSymbolPointer<KtVariableLikeSymbol> = withValidityAssertion {
        val accessorPsi = descriptor.containingDeclaration.toSourceElement.getPsi()
        if (accessorPsi is KtPropertyAccessor) {
            val accessorPointer = KtPsiBasedSymbolPointer<KtPropertyAccessorSymbol>(accessorPsi.createSmartPointer())
            return KtFe10DescSyntheticFieldSymbolPointer(accessorPointer)
        }

        return KtFe10NeverRestoringSymbolPointer()
    }
}