/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractEffectDeclaration
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.contracts.effectDeclarationToAnalysisApi
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10DescFunctionSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBasePsiSymbolPointer
import org.jetbrains.kotlin.analysis.api.impl.base.util.kotlinFunctionInvokeCallableIds
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.contracts.description.ContractProviderKey
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.inference.returnTypeOrNothing
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension

internal class KaFe10DescNamedFunctionSymbol private constructor(
    override val descriptor: FunctionDescriptor,
    override val analysisContext: Fe10AnalysisContext,
) : KaNamedFunctionSymbol(), KaFe10DescSymbol<FunctionDescriptor> {
    override val name: Name
        get() = withValidityAssertion { descriptor.name }

    override val contractEffects: List<KaContractEffectDeclaration> by cached {
        descriptor.getUserData(ContractProviderKey)?.getContractDescription()?.effects
            ?.map { it.effectDeclarationToAnalysisApi(analysisContext) }
            .orEmpty()
    }

    override val location: KaSymbolLocation
        get() = withValidityAssertion { descriptor.kaSymbolLocation }

    override val modality: KaSymbolModality
        get() = withValidityAssertion { descriptor.kaSymbolModality }

    override val compilerVisibility: Visibility
        get() = withValidityAssertion { descriptor.ktVisibility }

    override val isSuspend: Boolean
        get() = withValidityAssertion { descriptor.isSuspend }

    override val isOperator: Boolean
        get() = withValidityAssertion {
            if (descriptor.isDynamic()) {
                // For consistency with the K2 implementation, see `FirDynamicMembersStorage`
                return@withValidityAssertion true
            }
            descriptor.isOperator
        }

    override val isExternal: Boolean
        get() = withValidityAssertion { descriptor.isExternal }

    override val isInline: Boolean
        get() = withValidityAssertion { descriptor.isInline }

    override val isOverride: Boolean
        get() = withValidityAssertion { descriptor.isExplicitOverride }

    override val isInfix: Boolean
        get() = withValidityAssertion {
            if (descriptor.isDynamic()) {
                // For consistency with the K2 implementation, see `FirDynamicMembersStorage`
                return@withValidityAssertion true
            }
            descriptor.isInfix
        }

    override val isStatic: Boolean
        get() = withValidityAssertion { descriptor is JavaCallableMemberDescriptor && DescriptorUtils.isStaticDeclaration(descriptor) }

    override val isTailRec: Boolean
        get() = withValidityAssertion { descriptor.isTailrec }

    override val isBuiltinFunctionInvoke: Boolean
        get() = withValidityAssertion { callableId in kotlinFunctionInvokeCallableIds }

    override val isActual: Boolean
        get() = withValidityAssertion { descriptor.isActual }

    override val isExpect: Boolean
        get() = withValidityAssertion { descriptor.isExpect }

    override val valueParameters: List<KaValueParameterSymbol>
        get() = withValidityAssertion {
            if (descriptor.isDynamic()) {
                // For consistency with the K2 implementation, see `FirDynamicMembersStorage`
                return@withValidityAssertion listOf(KaFe10DynamicFunctionDescValueParameterSymbol(this))
            }
            descriptor.valueParameters.map { KaFe10DescValueParameterSymbol(it, analysisContext) }
        }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { descriptor.ktHasStableParameterNames }

    override val callableId: CallableId?
        get() = withValidityAssertion { descriptor.callableIdIfNotLocal }

    override val returnType: KaType
        get() = withValidityAssertion { descriptor.returnTypeOrNothing.toKtType(analysisContext) }

    override val receiverParameter: KaReceiverParameterSymbol?
        get() = withValidityAssertion { descriptor.extensionReceiverParameter?.toKtReceiverParameterSymbol(analysisContext) }

    override val contextReceivers: List<KaContextReceiver>
        get() = withValidityAssertion { descriptor.createContextReceivers(analysisContext) }

    override val isExtension: Boolean
        get() = withValidityAssertion { descriptor.isExtension }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion { descriptor.typeParameters.map { KaFe10DescTypeParameterSymbol(it, analysisContext) } }

    override fun createPointer(): KaSymbolPointer<KaNamedFunctionSymbol> = withValidityAssertion {
        KaBasePsiSymbolPointer.createForSymbolFromSource<KaNamedFunctionSymbol>(this)?.let {
            return it
        }

        val callableId = descriptor.callableIdIfNotLocal
        if (callableId != null) {
            val signature = descriptor.getSymbolPointerSignature()
            return KaFe10DescFunctionSymbolPointer(callableId, signature, this)
        }

        return KaFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()

    companion object {
        fun build(descriptor: FunctionDescriptor, analysisContext: Fe10AnalysisContext): KaFe10DescNamedFunctionSymbol {
            return KaFe10DescNamedFunctionSymbol(descriptor, analysisContext)
        }
    }
}
