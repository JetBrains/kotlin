/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.KaFe10DescSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktVisibility
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBaseReceiverParameterSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibility

internal class KaFe10ReceiverParameterSymbol(
    override val descriptor: ReceiverParameterDescriptor,
    override val analysisContext: Fe10AnalysisContext,
) : KaReceiverParameterSymbol(), KaFe10DescSymbol<ReceiverParameterDescriptor> {
    override val returnType: KaType
        get() = withValidityAssertion {
            descriptor.returnType?.toKtType(analysisContext) ?: error("expect return type for $descriptor")
        }

    override val owningCallableSymbol: KaCallableSymbol
        get() = withValidityAssertion { descriptor.containingDeclaration.toKtSymbol(analysisContext) as KaCallableSymbol }

    @KaExperimentalApi
    override val compilerVisibility: Visibility
        get() = withValidityAssertion { descriptor.ktVisibility }

    override fun createPointer(): KaSymbolPointer<KaReceiverParameterSymbol> = withValidityAssertion {
        KaBaseReceiverParameterSymbolPointer(owningCallableSymbol.createPointer(), this)
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}