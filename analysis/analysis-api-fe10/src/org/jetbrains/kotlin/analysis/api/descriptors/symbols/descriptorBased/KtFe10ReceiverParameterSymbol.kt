/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.KtFe10DescSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor

class KtFe10ReceiverParameterSymbol(
    private val _descriptor: ReceiverParameterDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KtReceiverParameterSymbol(), KtFe10DescSymbol<ReceiverParameterDescriptor> {

    override val type: KtType
        get() = withValidityAssertion { _descriptor.returnType?.toKtType(analysisContext) ?: error("expect return type for $_descriptor") }
    override val descriptor: ReceiverParameterDescriptor
        get() = withValidityAssertion { _descriptor }

    override val owningCallableSymbol: KtCallableSymbol
        get() = withValidityAssertion { _descriptor.containingDeclaration.toKtSymbol(analysisContext) as KtCallableSymbol }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtReceiverParameterSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource<KtReceiverParameterSymbol>(this) ?: KtFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}