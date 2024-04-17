/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KtFe10AnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithDeclarations
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.psi.KtFile

internal class KtFe10FileSymbol(
    private val file: KtFile,
    override val analysisContext: Fe10AnalysisContext
) : KtFileSymbol(), KtSymbolWithDeclarations, KtFe10AnnotatedSymbol {
    override val psi: KtFile
        get() = withValidityAssertion { file }

    override val annotationsObject: Annotations
        get() = withValidityAssertion { analysisContext.resolveSession.getFileAnnotations(file) }

    override val origin: KtSymbolOrigin
        get() = withValidityAssertion { if (file.isCompiled) KtSymbolOrigin.LIBRARY else KtSymbolOrigin.SOURCE }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtFileSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource<KtFileSymbol>(this) ?: KtFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean {
        return this.file == (other as? KtFe10FileSymbol)?.file
    }

    override fun hashCode(): Int {
        return file.hashCode()
    }
}
