/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KaFe10AnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithDeclarations
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.psi.KtFile

internal class KaFe10FileSymbol(
    private val file: KtFile,
    override val analysisContext: Fe10AnalysisContext
) : KaFileSymbol(), KaSymbolWithDeclarations, KaFe10AnnotatedSymbol {
    override val psi: KtFile
        get() = withValidityAssertion { file }

    override val annotationsObject: Annotations
        get() = withValidityAssertion { analysisContext.resolveSession.getFileAnnotations(file) }

    override val origin: KaSymbolOrigin
        get() = withValidityAssertion { if (file.isCompiled) KaSymbolOrigin.LIBRARY else KaSymbolOrigin.SOURCE }

    override fun createPointer(): KaSymbolPointer<KaFileSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaFileSymbol>(this) ?: KaFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean {
        return this.file == (other as? KaFe10FileSymbol)?.file
    }

    override fun hashCode(): Int {
        return file.hashCode()
    }
}
