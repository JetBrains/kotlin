/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.base.KtContextReceiver
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.createContextReceivers
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.KtFe10PsiSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.createErrorType
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtAnonymousFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.resolve.BindingContext

internal class KtFe10PsiAnonymousFunctionSymbol(
    override val psi: KtNamedFunction,
    override val analysisContext: Fe10AnalysisContext
) : KtAnonymousFunctionSymbol(), KtFe10PsiSymbol<KtNamedFunction, FunctionDescriptor> {
    override val descriptor: FunctionDescriptor? by cached {
        val bindingContext = analysisContext.analyze(psi, AnalysisMode.PARTIAL)
        bindingContext[BindingContext.FUNCTION, psi]
    }

    override val valueParameters: List<KtValueParameterSymbol>
        get() = withValidityAssertion { psi.valueParameters.map { KtFe10PsiValueParameterSymbol(it, analysisContext) } }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { true }

    override val returnType: KtType
        get() = withValidityAssertion {
            descriptor?.returnType?.toKtType(analysisContext) ?: createErrorType()
        }

    override val receiverParameter: KtReceiverParameterSymbol?
        get() = withValidityAssertion {
            if (!psi.isExtensionDeclaration()) {
                return null
            }

            return descriptor?.extensionReceiverParameter?.toKtReceiverParameterSymbol(analysisContext)
        }

    override val contextReceivers: List<KtContextReceiver>
        get() = withValidityAssertion { descriptor?.createContextReceivers(analysisContext) ?: emptyList() }

    override val isExtension: Boolean
        get() = withValidityAssertion { psi.isExtensionDeclaration() }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtAnonymousFunctionSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource<KtAnonymousFunctionSymbol>(this) ?: KtFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}
