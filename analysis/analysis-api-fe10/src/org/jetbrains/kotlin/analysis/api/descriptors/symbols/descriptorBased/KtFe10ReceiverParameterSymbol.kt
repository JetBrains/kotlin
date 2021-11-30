/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.KtFe10DescSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.symbols.KtReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor

class KtFe10ReceiverParameterSymbol(
    private val _descriptor: ReceiverParameterDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KtReceiverParameterSymbol(), KtFe10DescSymbol<ReceiverParameterDescriptor> {

    override val type: KtType
        get() = withValidityAssertion { _descriptor.returnType?.toKtType(analysisContext) ?: error("expect return type for $_descriptor") }
    override val descriptor: ReceiverParameterDescriptor
        get() = withValidityAssertion { _descriptor }

    override fun createPointer(): KtSymbolPointer<KtSymbol> {
        TODO("Not yet implemented")
    }
}