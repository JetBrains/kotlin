/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KtFe10DescValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.KtFe10PsiSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.createErrorType
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.symbols.KtAnonymousFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.resolve.BindingContext

internal class KtFe10PsiLiteralAnonymousFunctionSymbol(
    override val psi: KtFunctionLiteral,
    override val analysisContext: Fe10AnalysisContext
) : KtAnonymousFunctionSymbol(), KtFe10PsiSymbol<KtFunctionLiteral, FunctionDescriptor> {
    override val descriptor: FunctionDescriptor? by cached {
        val bindingContext = analysisContext.analyze(psi)
        bindingContext[BindingContext.FUNCTION, psi]
    }

    override val valueParameters: List<KtValueParameterSymbol>
        get() = withValidityAssertion {
            return if (psi.valueParameters.isNotEmpty()) {
                psi.valueParameters.map { KtFe10PsiValueParameterSymbol(it, analysisContext) }
            } else {
                // There might be implicit 'it'
                descriptor?.valueParameters.orEmpty().map { KtFe10DescValueParameterSymbol(it, analysisContext) }
            }
        }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { true }

    override val returnType: KtType
        get() = withValidityAssertion {
            descriptor?.returnType?.toKtType(analysisContext) ?: createErrorType()
        }

    override val receiverType: KtType?
        get() = withValidityAssertion {
            val descriptor = this.descriptor ?: return createErrorType()
            val extensionReceiverParameter = descriptor.extensionReceiverParameter ?: return null
            extensionReceiverParameter.type.toKtType(analysisContext)
        }

    override val isExtension: Boolean
        get() = withValidityAssertion { psi.isExtensionDeclaration() }

    override fun createPointer(): KtSymbolPointer<KtAnonymousFunctionSymbol> = withValidityAssertion {
        return KtPsiBasedSymbolPointer.createForSymbolFromSource(this) ?: KtFe10NeverRestoringSymbolPointer()
    }
}