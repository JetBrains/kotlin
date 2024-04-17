/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KtFe10AnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.KtFe10PsiSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtScriptSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.resolve.BindingContext

internal class KtFe10PsiScriptSymbol(
    private val script: KtScript,
    override val analysisContext: Fe10AnalysisContext
) : KtScriptSymbol(), KtFe10AnnotatedSymbol, KtFe10PsiSymbol<KtScript, ScriptDescriptor> {
    override val descriptor: ScriptDescriptor? by cached {
        val bindingContext = analysisContext.analyze(psi, Fe10AnalysisFacade.AnalysisMode.PARTIAL)
        bindingContext[BindingContext.SCRIPT, psi]
    }

    override val origin: KtSymbolOrigin
        get() = withValidityAssertion { if (script.containingKtFile.isCompiled) KtSymbolOrigin.LIBRARY else KtSymbolOrigin.SOURCE }

    override val psi: KtScript
        get() = withValidityAssertion { script }

    override val name: Name
        get() = withValidityAssertion { script.nameAsSafeName }

    override val typeParameters: List<KtTypeParameterSymbol>
        get() = withValidityAssertion { emptyList() }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtScriptSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource<KtScriptSymbol>(this) ?: KtFe10NeverRestoringSymbolPointer()
    }
}
