/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10DescSamConstructorSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.inference.returnTypeOrNothing
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.sam.SamConstructorDescriptor
import org.jetbrains.kotlin.resolve.sam.SamTypeAliasConstructorDescriptor

internal class KaFe10DescSamConstructorSymbol(
    override val descriptor: SamConstructorDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KaSamConstructorSymbol(), KaFe10DescSymbol<SamConstructorDescriptor> {
    private val expandedDescriptor: SamConstructorDescriptor
        get() = (descriptor as? SamTypeAliasConstructorDescriptor)?.expandedConstructorDescriptor ?: descriptor
    override val name: Name
        get() = withValidityAssertion { expandedDescriptor.name }

    override val valueParameters: List<KaValueParameterSymbol>
        get() = withValidityAssertion { descriptor.valueParameters.map { KaFe10DescValueParameterSymbol(it, analysisContext) } }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { descriptor.ktHasStableParameterNames }

    override val callableId: CallableId?
        get() = withValidityAssertion { expandedDescriptor.callableIdIfNotLocal }

    override val returnType: KaType
        get() = withValidityAssertion { descriptor.returnTypeOrNothing.toKtType(analysisContext) }

    override val receiverParameter: KaReceiverParameterSymbol?
        get() = withValidityAssertion { descriptor.extensionReceiverParameter?.toKtReceiverParameterSymbol(analysisContext) }

    override val contextReceivers: List<KaContextReceiver>
        get() = withValidityAssertion { descriptor.createContextReceivers(analysisContext) }

    override val isExtension: Boolean
        get() = withValidityAssertion { descriptor.isExtension }

    override val origin: KaSymbolOrigin
        get() = withValidityAssertion { expandedDescriptor.getSymbolOrigin(analysisContext) }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion { descriptor.typeParameters.map { KaFe10DescTypeParameterSymbol(it, analysisContext) } }


    override fun createPointer(): KaSymbolPointer<KaSamConstructorSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaSamConstructorSymbol>(this)?.let {
            return it
        }

        val classId = descriptor.baseDescriptorForSynthetic.classId
        if (classId != null) {
            return KaFe10DescSamConstructorSymbolPointer(classId)
        }

        return KaFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}