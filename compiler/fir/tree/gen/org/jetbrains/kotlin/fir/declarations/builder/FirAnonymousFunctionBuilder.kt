/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirAnonymousFunctionImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

@FirBuilderDsl
class FirAnonymousFunctionBuilder : FirFunctionBuilder, FirTypeParametersOwnerBuilder, FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    override var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    override lateinit var moduleData: FirModuleData
    override lateinit var origin: FirDeclarationOrigin
    override var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    override var status: FirDeclarationStatus = FirResolvedDeclarationStatusImpl.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
    override lateinit var returnTypeRef: FirTypeRef
    var receiverParameter: FirReceiverParameter? = null
    override var deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider
    override var dispatchReceiverType: ConeSimpleKotlinType? = null
    override val contextParameters: MutableList<FirValueParameter> = mutableListOf()
    var controlFlowGraphReference: FirControlFlowGraphReference? = null
    override val valueParameters: MutableList<FirValueParameter> = mutableListOf()
    override var body: FirBlock? = null
    var contractDescription: FirContractDescription? = null
    lateinit var symbol: FirAnonymousFunctionSymbol
    var label: FirLabel? = null
    var invocationKind: EventOccurrencesRange? = null
    var inlineStatus: InlineStatus = InlineStatus.Unknown
    var isLambda: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var hasExplicitParameterList: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    override val typeParameters: MutableList<FirTypeParameter> = mutableListOf()
    var typeRef: FirTypeRef = FirImplicitTypeRefImplWithoutSource

    override fun build(): FirAnonymousFunction {
        return FirAnonymousFunctionImpl(
            source,
            resolvePhase,
            annotations.toMutableOrEmpty(),
            moduleData,
            origin,
            attributes,
            status,
            returnTypeRef,
            receiverParameter,
            deprecationsProvider,
            dispatchReceiverType,
            contextParameters.toMutableOrEmpty(),
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


    @Deprecated("Modification of 'isLocal' has no impact for FirAnonymousFunctionBuilder", level = DeprecationLevel.HIDDEN)
    override var isLocal: Boolean
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'containerSource' has no impact for FirAnonymousFunctionBuilder", level = DeprecationLevel.HIDDEN)
    override var containerSource: DeserializedContainerSource?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildAnonymousFunction(init: FirAnonymousFunctionBuilder.() -> Unit): FirAnonymousFunction {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirAnonymousFunctionBuilder().apply(init).build()
}

@OptIn(FirImplementationDetail::class)
fun buildAnonymousFunction(
    source: KtSourceElement? = null,
    resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR,
    annotations: MutableList<FirAnnotation> = mutableListOf(),
    moduleData: FirModuleData,
    origin: FirDeclarationOrigin,
    attributes: FirDeclarationAttributes = FirDeclarationAttributes(),
    status: FirDeclarationStatus = FirResolvedDeclarationStatusImpl.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS,
    returnTypeRef: FirTypeRef,
    receiverParameter: FirReceiverParameter? = null,
    deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider,
    dispatchReceiverType: ConeSimpleKotlinType? = null,
    contextParameters: MutableList<FirValueParameter> = mutableListOf(),
    controlFlowGraphReference: FirControlFlowGraphReference? = null,
    valueParameters: MutableList<FirValueParameter> = mutableListOf(),
    body: FirBlock? = null,
    contractDescription: FirContractDescription? = null,
    symbol: FirAnonymousFunctionSymbol,
    label: FirLabel? = null,
    invocationKind: EventOccurrencesRange? = null,
    inlineStatus: InlineStatus = InlineStatus.Unknown,
    isLambda: Boolean,
    hasExplicitParameterList: Boolean,
    typeParameters: MutableList<FirTypeParameter> = mutableListOf(),
    typeRef: FirTypeRef = FirImplicitTypeRefImplWithoutSource,
): FirAnonymousFunction {
    return FirAnonymousFunctionImpl(
        source,
        resolvePhase,
        annotations.toMutableOrEmpty(),
        moduleData,
        origin,
        attributes,
        status,
        returnTypeRef,
        receiverParameter,
        deprecationsProvider,
        dispatchReceiverType,
        contextParameters.toMutableOrEmpty(),
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

@OptIn(FirImplementationDetail::class)
fun buildAnonymousFunctionCopy(
    original: FirAnonymousFunction,
    source: KtSourceElement? = original.source,
    resolvePhase: FirResolvePhase = original.resolvePhase,
    annotations: MutableList<FirAnnotation> = original.annotations.toMutableList(),
    moduleData: FirModuleData = original.moduleData,
    origin: FirDeclarationOrigin = original.origin,
    attributes: FirDeclarationAttributes = original.attributes.copy(),
    status: FirDeclarationStatus = original.status,
    returnTypeRef: FirTypeRef = original.returnTypeRef,
    receiverParameter: FirReceiverParameter? = original.receiverParameter,
    deprecationsProvider: DeprecationsProvider = original.deprecationsProvider,
    dispatchReceiverType: ConeSimpleKotlinType? = original.dispatchReceiverType,
    contextParameters: MutableList<FirValueParameter> = original.contextParameters.toMutableList(),
    controlFlowGraphReference: FirControlFlowGraphReference? = original.controlFlowGraphReference,
    valueParameters: MutableList<FirValueParameter> = original.valueParameters.toMutableList(),
    body: FirBlock? = original.body,
    contractDescription: FirContractDescription? = original.contractDescription,
    symbol: FirAnonymousFunctionSymbol,
    label: FirLabel? = original.label,
    invocationKind: EventOccurrencesRange? = original.invocationKind,
    inlineStatus: InlineStatus = original.inlineStatus,
    isLambda: Boolean = original.isLambda,
    hasExplicitParameterList: Boolean = original.hasExplicitParameterList,
    typeParameters: MutableList<FirTypeParameter> = original.typeParameters.toMutableList(),
    typeRef: FirTypeRef = original.typeRef,
): FirAnonymousFunction {
    return FirAnonymousFunctionImpl(
        source,
        resolvePhase,
        annotations.toMutableOrEmpty(),
        moduleData,
        origin,
        attributes,
        status,
        returnTypeRef,
        receiverParameter,
        deprecationsProvider,
        dispatchReceiverType,
        contextParameters.toMutableOrEmpty(),
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
