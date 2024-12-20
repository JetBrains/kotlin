/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractEffectDeclaration
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.contracts.effectDeclarationToAnalysisApi
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBasePsiSymbolPointer
import org.jetbrains.kotlin.analysis.api.impl.base.util.kotlinFunctionInvokeCallableIds
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.contracts.description.ContractProviderKey
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.resolve.BindingContext

internal class KaFe10PsiNamedFunctionSymbol(
    override val psi: KtNamedFunction,
    override val analysisContext: Fe10AnalysisContext
) : KaNamedFunctionSymbol(), KaFe10PsiSymbol<KtNamedFunction, FunctionDescriptor> {
    override val descriptor: FunctionDescriptor? by cached {
        val bindingContext = analysisContext.analyze(psi, AnalysisMode.PARTIAL)
        bindingContext[BindingContext.FUNCTION, psi]
    }

    override val contractEffects: List<KaContractEffectDeclaration> by cached {
        descriptor?.getUserData(ContractProviderKey)?.getContractDescription()?.effects
            ?.map { it.effectDeclarationToAnalysisApi(analysisContext) }
            .orEmpty()
    }

    override val isSuspend: Boolean
        get() = withValidityAssertion { psi.hasModifier(KtTokens.SUSPEND_KEYWORD) }

    override val isOperator: Boolean
        get() = withValidityAssertion { psi.hasModifier(KtTokens.OPERATOR_KEYWORD) }

    override val isExternal: Boolean
        get() = withValidityAssertion { psi.hasModifier(KtTokens.EXTERNAL_KEYWORD) }

    override val isInline: Boolean
        get() = withValidityAssertion { psi.hasModifier(KtTokens.INLINE_KEYWORD) }

    override val isOverride: Boolean
        get() = withValidityAssertion { psi.hasModifier(KtTokens.OVERRIDE_KEYWORD) }

    override val isInfix: Boolean
        get() = withValidityAssertion { psi.hasModifier(KtTokens.INFIX_KEYWORD) }

    override val isStatic: Boolean
        get() = withValidityAssertion { false }

    override val isTailRec: Boolean
        get() = withValidityAssertion { psi.hasModifier(KtTokens.TAILREC_KEYWORD) }

    override val isBuiltinFunctionInvoke: Boolean
        get() = withValidityAssertion { callableId in kotlinFunctionInvokeCallableIds }

    override val isActual: Boolean
        get() = withValidityAssertion { descriptor?.isActual ?: psi.hasActualModifier() }

    override val isExpect: Boolean
        get() = withValidityAssertion { descriptor?.isExpect ?: psi.hasExpectModifier() }

    override val valueParameters: List<KaValueParameterSymbol>
        get() = withValidityAssertion { psi.valueParameters.map { KaFe10PsiValueParameterSymbol(it, analysisContext) } }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { true }

    override val callableId: CallableId?
        get() = withValidityAssertion { psi.callableId }

    override val returnType: KaType
        get() = withValidityAssertion { descriptor?.returnType?.toKtType(analysisContext) ?: createErrorType() }

    override val receiverParameter: KaReceiverParameterSymbol?
        get() = withValidityAssertion {
            if (!psi.isExtensionDeclaration()) {
                return null
            }

            return descriptor?.extensionReceiverParameter?.toKtReceiverParameterSymbol(analysisContext)
        }

    override val contextReceivers: List<KaContextReceiver>
        get() = withValidityAssertion { descriptor?.createContextReceivers(analysisContext) ?: emptyList() }

    override val isExtension: Boolean
        get() = withValidityAssertion { psi.isExtensionDeclaration() }

    override val location: KaSymbolLocation
        get() = withValidityAssertion { psi.kaSymbolLocation }

    override val name: Name
        get() = withValidityAssertion { psi.nameAsSafeName }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion { psi.typeParameters.map { KaFe10PsiTypeParameterSymbol(it, analysisContext) } }

    override val modality: KaSymbolModality
        get() = withValidityAssertion { psi.kaSymbolModality ?: descriptor?.kaSymbolModality ?: KaSymbolModality.FINAL }

    override val compilerVisibility: Visibility
        get() = withValidityAssertion { psi.ktVisibility ?: descriptor?.ktVisibility ?: Visibilities.Public }

    override fun createPointer(): KaSymbolPointer<KaNamedFunctionSymbol> = withValidityAssertion {
        KaBasePsiSymbolPointer.createForSymbolFromSource<KaNamedFunctionSymbol>(this) ?: KaFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}
