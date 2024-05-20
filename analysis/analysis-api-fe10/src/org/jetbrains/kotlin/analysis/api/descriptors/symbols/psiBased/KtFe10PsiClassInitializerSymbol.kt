/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationsList
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KaFe10Symbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaEmptyAnnotationsList
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassInitializerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolKind
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

    override val symbolKind: KaSymbolKind
        get() = withValidityAssertion { KaSymbolKind.CLASS_MEMBER }

    override fun createPointer(): KaSymbolPointer<KaClassInitializerSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaClassInitializerSymbol>(this) ?: KaFe10NeverRestoringSymbolPointer()
    }

    override val typeParameters: List<KaTypeParameterSymbol> get() = withValidityAssertion { emptyList() }
    override val annotationsList: KaAnnotationsList get() = withValidityAssertion { KaEmptyAnnotationsList(token) }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}
