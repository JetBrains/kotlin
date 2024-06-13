/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KaFe10AnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.KaFe10PsiSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaScriptSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.resolve.BindingContext

internal class KaFe10PsiScriptSymbol(
    private val script: KtScript,
    override val analysisContext: Fe10AnalysisContext
) : KaScriptSymbol(), KaFe10AnnotatedSymbol, KaFe10PsiSymbol<KtScript, ScriptDescriptor> {
    override val descriptor: ScriptDescriptor? by cached {
        val bindingContext = analysisContext.analyze(psi, Fe10AnalysisFacade.AnalysisMode.PARTIAL)
        bindingContext[BindingContext.SCRIPT, psi]
    }

    override val origin: KaSymbolOrigin
        get() = withValidityAssertion { if (script.containingKtFile.isCompiled) KaSymbolOrigin.LIBRARY else KaSymbolOrigin.SOURCE }

    override val psi: KtScript
        get() = withValidityAssertion { script }

    override val name: Name
        get() = withValidityAssertion { script.nameAsSafeName }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion { emptyList() }

    override fun createPointer(): KaSymbolPointer<KaScriptSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaScriptSymbol>(this) ?: KaFe10NeverRestoringSymbolPointer()
    }
}
