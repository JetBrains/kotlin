/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.KtFe10DescSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.calls.components.isVararg

internal class KtFe10DescValueParameterSymbol(
    override val descriptor: ValueParameterDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KtValueParameterSymbol(), KtFe10DescSymbol<ValueParameterDescriptor> {
    override val name: Name
        get() = withValidityAssertion {
            return when (val name = descriptor.name) {
                SpecialNames.IMPLICIT_SET_PARAMETER -> Name.identifier("value")
                else -> name
            }
        }

    override val hasDefaultValue: Boolean
        get() = withValidityAssertion { descriptor.hasDefaultValue() }

    override val isVararg: Boolean
        get() = withValidityAssertion { descriptor.isVararg }

    override val returnType: KtType
        get() = withValidityAssertion {
            return (descriptor.varargElementType ?: descriptor.type).toKtType(analysisContext)
        }

    override fun createPointer(): KtSymbolPointer<KtValueParameterSymbol> = withValidityAssertion {
        return KtPsiBasedSymbolPointer.createForSymbolFromSource(this) ?: KtFe10NeverRestoringSymbolPointer()
    }
}