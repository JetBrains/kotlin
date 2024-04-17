/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.KtFe10PsiSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.createErrorType
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtLocalVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.BindingContext

internal class KtFe10PsiLoopParameterLocalVariableSymbol(
    override val psi: KtParameter,
    override val analysisContext: Fe10AnalysisContext
) : KtLocalVariableSymbol(), KtFe10PsiSymbol<KtParameter, VariableDescriptor> {
    override val descriptor: VariableDescriptor? by cached {
        val bindingContext = analysisContext.analyze(psi, Fe10AnalysisFacade.AnalysisMode.PARTIAL)
        bindingContext[BindingContext.VALUE_PARAMETER, psi]
    }

    override val name: Name
        get() = withValidityAssertion { psi.nameAsSafeName }

    override val returnType: KtType
        get() = withValidityAssertion { descriptor?.type?.toKtType(analysisContext) ?: createErrorType() }

    override val isVal: Boolean
        get() = withValidityAssertion { !psi.isMutable }

    override val symbolKind: KtSymbolKind
        get() = withValidityAssertion { KtSymbolKind.LOCAL }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtLocalVariableSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource<KtLocalVariableSymbol>(this) ?: KtFe10NeverRestoringSymbolPointer()
    }


    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}
