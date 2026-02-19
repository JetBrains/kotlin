/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.KaFe10PsiSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.createErrorType
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBasePsiSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaContextParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.calls.util.isSingleUnderscore

internal class KaFe10PsiContextParameterSymbol(
    override val psi: KtParameter,
    override val analysisContext: Fe10AnalysisContext,
) : KaContextParameterSymbol(), KaFe10PsiSymbol<KtParameter, ReceiverParameterDescriptor> {
    init {
        require(psi.isContextParameter)
    }

    override val descriptor: ReceiverParameterDescriptor? get() = null

    override val compilerVisibility: Visibility
        get() = withValidityAssertion { Visibilities.Public }

    override val returnType: KaType
        get() = withValidityAssertion { createErrorType() }

    override val name: Name
        get() = withValidityAssertion {
            if (psi.isSingleUnderscore) {
                SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
            } else {
                psi.nameAsSafeName
            }
        }

    override fun createPointer(): KaSymbolPointer<KaContextParameterSymbol> = withValidityAssertion {
        KaBasePsiSymbolPointer.createForSymbolFromSource<KaContextParameterSymbol>(this) ?: KaFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}
