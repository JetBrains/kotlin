/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.impl.FirEmptyContractDescription
import org.jetbrains.kotlin.fir.declarations.DeprecationsProvider
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirContextReceiver
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirResolveState
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.InlineStatus
import org.jetbrains.kotlin.fir.declarations.ResolveStateAccess
import org.jetbrains.kotlin.fir.declarations.UnresolvedDeprecationProvider
import org.jetbrains.kotlin.fir.declarations.asResolveState
import org.jetbrains.kotlin.fir.declarations.builder.FirFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.impl.FirAnonymousFunctionImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.resolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirAnonymousFunctionBuilder : FirFunctionBuilder, FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    override var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    override lateinit var moduleData: FirModuleData
    override lateinit var origin: FirDeclarationOrigin
    override var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    override lateinit var returnTypeRef: FirTypeRef
    var receiverParameter: FirReceiverParameter? = null
    override var deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider
    override var containerSource: DeserializedContainerSource? = null
    override var dispatchReceiverType: ConeSimpleKotlinType? = null
    override val contextReceivers: MutableList<FirContextReceiver> = mutableListOf()
    var controlFlowGraphReference: FirControlFlowGraphReference? = null
    override val valueParameters: MutableList<FirValueParameter> = mutableListOf()
    override var body: FirBlock? = null
    var contractDescription: FirContractDescription = FirEmptyContractDescription
    lateinit var symbol: FirAnonymousFunctionSymbol
    var label: FirLabel? = null
    var invocationKind: EventOccurrencesRange? = null
    var inlineStatus: InlineStatus = InlineStatus.Unknown
    var isLambda: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var hasExplicitParameterList: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    val typeParameters: MutableList<FirTypeParameter> = mutableListOf()
    var typeRef: FirTypeRef = FirImplicitTypeRefImplWithoutSource

    override fun build(): FirAnonymousFunction {
        return FirAnonymousFunctionImpl(
            source,
            resolvePhase,
            annotations.toMutableOrEmpty(),
            moduleData,
            origin,
            attributes,
            returnTypeRef,
            receiverParameter,
            deprecationsProvider,
            containerSource,
            dispatchReceiverType,
            contextReceivers.toMutableOrEmpty(),
            controlFlowGraphReference,
            valueParameters,
            body,
            contractDescription,
            symbol,
            label,
            invocationKind,
            inlineStatus,
            isLambda,
            hasExplicitParameterList,
            typeParameters,
            typeRef,
        )
    }


    @Deprecated("Modification of 'status' has no impact for FirAnonymousFunctionBuilder", level = DeprecationLevel.HIDDEN)
    override var status: FirDeclarationStatus
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildAnonymousFunction(init: FirAnonymousFunctionBuilder.() -> Unit): FirAnonymousFunction {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirAnonymousFunctionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildAnonymousFunctionCopy(original: FirAnonymousFunction, init: FirAnonymousFunctionBuilder.() -> Unit): FirAnonymousFunction {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirAnonymousFunctionBuilder()
    copyBuilder.source = original.source
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.moduleData = original.moduleData
    copyBuilder.origin = original.origin
    copyBuilder.attributes = original.attributes.copy()
    copyBuilder.returnTypeRef = original.returnTypeRef
    copyBuilder.receiverParameter = original.receiverParameter
    copyBuilder.deprecationsProvider = original.deprecationsProvider
    copyBuilder.containerSource = original.containerSource
    copyBuilder.dispatchReceiverType = original.dispatchReceiverType
    copyBuilder.contextReceivers.addAll(original.contextReceivers)
    copyBuilder.controlFlowGraphReference = original.controlFlowGraphReference
    copyBuilder.valueParameters.addAll(original.valueParameters)
    copyBuilder.body = original.body
    copyBuilder.contractDescription = original.contractDescription
    copyBuilder.symbol = original.symbol
    copyBuilder.label = original.label
    copyBuilder.invocationKind = original.invocationKind
    copyBuilder.inlineStatus = original.inlineStatus
    copyBuilder.isLambda = original.isLambda
    copyBuilder.hasExplicitParameterList = original.hasExplicitParameterList
    copyBuilder.typeParameters.addAll(original.typeParameters)
    copyBuilder.typeRef = original.typeRef
    return copyBuilder.apply(init).build()
}
