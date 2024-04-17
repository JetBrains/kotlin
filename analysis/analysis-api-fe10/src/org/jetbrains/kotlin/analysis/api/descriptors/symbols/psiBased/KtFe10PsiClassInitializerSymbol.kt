/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KtFe10Symbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KtEmptyAnnotationsList
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtClassInitializerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.psi.KtClassInitializer

class KtFe10PsiClassInitializerSymbol(
    override val psi: KtClassInitializer,
    override val analysisContext: Fe10AnalysisContext
) : KtClassInitializerSymbol(), KtFe10Symbol {
    override val origin: KtSymbolOrigin
        get() = withValidityAssertion {
            return if (psi.containingKtFile.isCompiled) {
                KtSymbolOrigin.LIBRARY
            } else {
                KtSymbolOrigin.SOURCE
            }
        }

    override val symbolKind: KtSymbolKind
        get() = withValidityAssertion { KtSymbolKind.CLASS_MEMBER }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtClassInitializerSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource<KtClassInitializerSymbol>(this) ?: KtFe10NeverRestoringSymbolPointer()
    }

    override val typeParameters: List<KtTypeParameterSymbol> get() = withValidityAssertion { emptyList() }
    override val annotationsList: KtAnnotationsList get() = withValidityAssertion { KtEmptyAnnotationsList(token) }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}
