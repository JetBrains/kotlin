/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.base.KtContextReceiver
import org.jetbrains.kotlin.analysis.api.contracts.description.KtContractEffectDeclaration
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.contracts.effectDeclarationToAnalysisApi
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10DescFunctionLikeSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.impl.base.util.kotlinFunctionInvokeCallableIds
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.contracts.description.ContractProviderKey
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.inference.returnTypeOrNothing
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension

internal class KtFe10DescFunctionSymbol private constructor(
    override val descriptor: FunctionDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KtFunctionSymbol(), KtFe10DescMemberSymbol<FunctionDescriptor> {
    override val name: Name
        get() = withValidityAssertion { descriptor.name }

    override val contractEffects: List<KtContractEffectDeclaration> by cached {
        descriptor.getUserData(ContractProviderKey)?.getContractDescription()?.effects
            ?.map { it.effectDeclarationToAnalysisApi(analysisContext) }
            .orEmpty()
    }

    override val symbolKind: KtSymbolKind
        get() = withValidityAssertion { descriptor.ktSymbolKind }

    override val isSuspend: Boolean
        get() = withValidityAssertion { descriptor.isSuspend }

    override val isOperator: Boolean
        get() = withValidityAssertion { descriptor.isOperator }

    override val isExternal: Boolean
        get() = withValidityAssertion { descriptor.isExternal }

    override val isInline: Boolean
        get() = withValidityAssertion { descriptor.isInline }

    override val isOverride: Boolean
        get() = withValidityAssertion { descriptor.isExplicitOverride }

    override val isInfix: Boolean
        get() = withValidityAssertion { descriptor.isInfix }

    override val isStatic: Boolean
        get() = withValidityAssertion { descriptor is JavaCallableMemberDescriptor && DescriptorUtils.isStaticDeclaration(descriptor) }

    override val isBuiltinFunctionInvoke: Boolean
        get() = withValidityAssertion { callableIdIfNonLocal in kotlinFunctionInvokeCallableIds }

    override val isActual: Boolean
        get() = withValidityAssertion { descriptor.isActual }

    override val isExpect: Boolean
        get() = withValidityAssertion { descriptor.isExpect }

    override val valueParameters: List<KtValueParameterSymbol>
        get() = withValidityAssertion { descriptor.valueParameters.map { KtFe10DescValueParameterSymbol(it, analysisContext) } }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { descriptor.ktHasStableParameterNames }

    override val callableIdIfNonLocal: CallableId?
        get() = withValidityAssertion { descriptor.callableIdIfNotLocal }

    override val returnType: KtType
        get() = withValidityAssertion { descriptor.returnTypeOrNothing.toKtType(analysisContext) }

    override val receiverParameter: KtReceiverParameterSymbol?
        get() = withValidityAssertion { descriptor.extensionReceiverParameter?.toKtReceiverParameterSymbol(analysisContext) }

    override val contextReceivers: List<KtContextReceiver>
        get() = withValidityAssertion { descriptor.createContextReceivers(analysisContext) }

    override val isExtension: Boolean
        get() = withValidityAssertion { descriptor.isExtension }

    override val typeParameters: List<KtTypeParameterSymbol>
        get() = withValidityAssertion { descriptor.typeParameters.map { KtFe10DescTypeParameterSymbol(it, analysisContext) } }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtFunctionSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource<KtFunctionSymbol>(this)?.let {
            return it
        }

        val callableId = descriptor.callableIdIfNotLocal
        if (callableId != null) {
            val signature = descriptor.getSymbolPointerSignature()
            return KtFe10DescFunctionLikeSymbolPointer(callableId, signature)
        }

        return KtFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()

    companion object {
        fun build(descriptor: FunctionDescriptor, analysisContext: Fe10AnalysisContext): KtFe10DescFunctionSymbol {
            return KtFe10DescFunctionSymbol(descriptor, analysisContext)
        }
    }
}
