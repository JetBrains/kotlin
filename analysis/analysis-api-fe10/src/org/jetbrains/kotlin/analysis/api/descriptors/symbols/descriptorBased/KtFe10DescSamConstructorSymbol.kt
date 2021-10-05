/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10DescSamConstructorSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.KtSamConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtTypeAndAnnotations
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.inference.returnTypeOrNothing
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.sam.SamConstructorDescriptor

internal class KtFe10DescSamConstructorSymbol(
    override val descriptor: SamConstructorDescriptor,
    override val analysisSession: KtFe10AnalysisSession
) : KtSamConstructorSymbol(), KtFe10DescSymbol<SamConstructorDescriptor> {
    override val name: Name
        get() = withValidityAssertion { descriptor.name }

    override val valueParameters: List<KtValueParameterSymbol>
        get() = withValidityAssertion { descriptor.valueParameters.map { KtFe10DescValueParameterSymbol(it, analysisSession) } }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { descriptor.ktHasStableParameterNames }

    override val callableIdIfNonLocal: CallableId?
        get() = withValidityAssertion { descriptor.callableId }

    override val annotatedType: KtTypeAndAnnotations
        get() = withValidityAssertion { descriptor.returnTypeOrNothing.toKtTypeAndAnnotations(analysisSession) }

    override val receiverType: KtTypeAndAnnotations?
        get() = withValidityAssertion { descriptor.extensionReceiverParameter?.type?.toKtTypeAndAnnotations(analysisSession) }

    override val isExtension: Boolean
        get() = withValidityAssertion { descriptor.isExtension }

    override val origin: KtSymbolOrigin
        get() = withValidityAssertion { KtSymbolOrigin.SAM_CONSTRUCTOR }

    override fun createPointer(): KtSymbolPointer<KtSamConstructorSymbol> = withValidityAssertion {
        val pointerByPsi = KtPsiBasedSymbolPointer.createForSymbolFromSource(this)
        if (pointerByPsi != null) {
            return pointerByPsi
        }

        val classId = descriptor.baseDescriptorForSynthetic.classId
        if (classId != null) {
            return KtFe10DescSamConstructorSymbolPointer(classId)
        }

        return KtFe10NeverRestoringSymbolPointer()
    }
}