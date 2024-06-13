/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KaFe10Symbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaEmptyAnnotationList
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassInitializerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolLocation
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.psi.KtClassInitializer

class KaFe10PsiClassInitializerSymbol(
    override val psi: KtClassInitializer,
    override val analysisContext: Fe10AnalysisContext
) : KaClassInitializerSymbol(), KaFe10Symbol {
    override val origin: KaSymbolOrigin
        get() = withValidityAssertion {
            return if (psi.containingKtFile.isCompiled) {
                KaSymbolOrigin.LIBRARY
            } else {
                KaSymbolOrigin.SOURCE
            }
        }

    override val location: KaSymbolLocation
        get() = withValidityAssertion { KaSymbolLocation.CLASS }

    override fun createPointer(): KaSymbolPointer<KaClassInitializerSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaClassInitializerSymbol>(this) ?: KaFe10NeverRestoringSymbolPointer()
    }

    override val typeParameters: List<KaTypeParameterSymbol> get() = withValidityAssertion { emptyList() }
    override val annotations: KaAnnotationList get() = withValidityAssertion { KaEmptyAnnotationList(token) }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}
