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
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirNamedFunctionImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

@FirBuilderDsl
open class FirNamedFunctionBuilder : FirFunctionBuilder, FirTypeParametersOwnerBuilder, FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    override var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override lateinit var moduleData: FirModuleData
    override lateinit var origin: FirDeclarationOrigin
    override var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    override lateinit var status: FirDeclarationStatus
    override var isLocal: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    override lateinit var returnTypeRef: FirTypeRef
    open var receiverParameter: FirReceiverParameter? = null
    override var deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider
    override var containerSource: DeserializedContainerSource? = null
    override var dispatchReceiverType: ConeSimpleKotlinType? = null
    override val contextParameters: MutableList<FirValueParameter> = mutableListOf()
    override val valueParameters: MutableList<FirValueParameter> = mutableListOf()
    override var body: FirBlock? = null
    open var contractDescription: FirContractDescription? = null
    open lateinit var name: Name
    open lateinit var symbol: FirNamedFunctionSymbol
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    override val typeParameters: MutableList<FirTypeParameter> = mutableListOf()

    override fun build(): FirNamedFunction {
        return FirNamedFunctionImpl(
            source,
            resolvePhase,
            moduleData,
            origin,
            attributes,
            status,
            isLocal,
            returnTypeRef,
            receiverParameter,
            deprecationsProvider,
            containerSource,
            dispatchReceiverType,
            contextParameters.toMutableOrEmpty(),
            valueParameters,
            body,
            contractDescription,
            name,
            symbol,
            annotations.toMutableOrEmpty(),
            typeParameters,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildNamedFunction(init: FirNamedFunctionBuilder.() -> Unit): FirNamedFunction {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirNamedFunctionBuilder().apply(init).build()
}

@OptIn(FirImplementationDetail::class)
fun buildNamedFunction(
    source: KtSourceElement? = null,
    resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR,
    moduleData: FirModuleData,
    origin: FirDeclarationOrigin,
    attributes: FirDeclarationAttributes = FirDeclarationAttributes(),
    status: FirDeclarationStatus,
    isLocal: Boolean,
    returnTypeRef: FirTypeRef,
    receiverParameter: FirReceiverParameter? = null,
    deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider,
    containerSource: DeserializedContainerSource? = null,
    dispatchReceiverType: ConeSimpleKotlinType? = null,
    contextParameters: MutableList<FirValueParameter> = mutableListOf(),
    valueParameters: MutableList<FirValueParameter> = mutableListOf(),
    body: FirBlock? = null,
    contractDescription: FirContractDescription? = null,
    name: Name,
    symbol: FirNamedFunctionSymbol,
    annotations: MutableList<FirAnnotation> = mutableListOf(),
    typeParameters: MutableList<FirTypeParameter> = mutableListOf(),
): FirNamedFunction {
    return FirNamedFunctionImpl(
        source,
        resolvePhase,
        moduleData,
        origin,
        attributes,
        status,
        isLocal,
        returnTypeRef,
        receiverParameter,
        deprecationsProvider,
        containerSource,
        dispatchReceiverType,
        contextParameters.toMutableOrEmpty(),
        valueParameters,
        body,
        contractDescription,
        name,
        symbol,
        annotations.toMutableOrEmpty(),
        typeParameters,
    )
}

@OptIn(ExperimentalContracts::class)
inline fun buildNamedFunctionCopy(original: FirNamedFunction, init: FirNamedFunctionBuilder.() -> Unit): FirNamedFunction {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirNamedFunctionBuilder()
    copyBuilder.source = original.source
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.moduleData = original.moduleData
    copyBuilder.origin = original.origin
    copyBuilder.attributes = original.attributes.copy()
    copyBuilder.status = original.status
    copyBuilder.isLocal = original.isLocal
    copyBuilder.returnTypeRef = original.returnTypeRef
    copyBuilder.receiverParameter = original.receiverParameter
    copyBuilder.deprecationsProvider = original.deprecationsProvider
    copyBuilder.containerSource = original.containerSource
    copyBuilder.dispatchReceiverType = original.dispatchReceiverType
    copyBuilder.contextParameters.addAll(original.contextParameters)
    copyBuilder.valueParameters.addAll(original.valueParameters)
    copyBuilder.body = original.body
    copyBuilder.contractDescription = original.contractDescription
    copyBuilder.name = original.name
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.typeParameters.addAll(original.typeParameters)
    return copyBuilder.apply(init).build()
}

@OptIn(FirImplementationDetail::class)
fun buildNamedFunctionCopy(
    original: FirNamedFunction,
    source: KtSourceElement? = original.source,
    resolvePhase: FirResolvePhase = original.resolvePhase,
    moduleData: FirModuleData = original.moduleData,
    origin: FirDeclarationOrigin = original.origin,
    attributes: FirDeclarationAttributes = original.attributes.copy(),
    status: FirDeclarationStatus = original.status,
    isLocal: Boolean = original.isLocal,
    returnTypeRef: FirTypeRef = original.returnTypeRef,
    receiverParameter: FirReceiverParameter? = original.receiverParameter,
    deprecationsProvider: DeprecationsProvider = original.deprecationsProvider,
    containerSource: DeserializedContainerSource? = original.containerSource,
    dispatchReceiverType: ConeSimpleKotlinType? = original.dispatchReceiverType,
    contextParameters: MutableList<FirValueParameter> = original.contextParameters.toMutableList(),
    valueParameters: MutableList<FirValueParameter> = original.valueParameters.toMutableList(),
    body: FirBlock? = original.body,
    contractDescription: FirContractDescription? = original.contractDescription,
    name: Name = original.name,
    symbol: FirNamedFunctionSymbol,
    annotations: MutableList<FirAnnotation> = original.annotations.toMutableList(),
    typeParameters: MutableList<FirTypeParameter> = original.typeParameters.toMutableList(),
): FirNamedFunction {
    return FirNamedFunctionImpl(
        source,
        resolvePhase,
        moduleData,
        origin,
        attributes,
        status,
        isLocal,
        returnTypeRef,
        receiverParameter,
        deprecationsProvider,
        containerSource,
        dispatchReceiverType,
        contextParameters.toMutableOrEmpty(),
        valueParameters,
        body,
        contractDescription,
        name,
        symbol,
        annotations.toMutableOrEmpty(),
        typeParameters,
    )
}
