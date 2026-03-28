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
import org.jetbrains.kotlin.fir.declarations.impl.FirConstructorImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

@FirBuilderDsl
open class FirConstructorBuilder : FirAbstractConstructorBuilder, FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    override var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override lateinit var moduleData: FirModuleData
    override lateinit var origin: FirDeclarationOrigin
    override var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    override val typeParameters: MutableList<FirTypeParameterRef> = mutableListOf()
    override lateinit var status: FirDeclarationStatus
    override var isLocal: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    override lateinit var returnTypeRef: FirTypeRef
    override var receiverParameter: FirReceiverParameter? = null
    override var deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider
    override var containerSource: DeserializedContainerSource? = null
    override var dispatchReceiverType: ConeSimpleKotlinType? = null
    override val contextParameters: MutableList<FirValueParameter> = mutableListOf()
    override val valueParameters: MutableList<FirValueParameter> = mutableListOf()
    override var contractDescription: FirContractDescription? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    override lateinit var symbol: FirConstructorSymbol
    override var delegatedConstructor: FirDelegatedConstructorCall? = null
    override var body: FirBlock? = null

    override fun build(): FirConstructor {
        return FirConstructorImpl(
            source,
            resolvePhase,
            moduleData,
            origin,
            attributes,
            typeParameters,
            status,
            isLocal,
            returnTypeRef,
            receiverParameter,
            deprecationsProvider,
            containerSource,
            dispatchReceiverType,
            contextParameters.toMutableOrEmpty(),
            valueParameters,
            contractDescription,
            annotations.toMutableOrEmpty(),
            symbol,
            delegatedConstructor,
            body,
        )
    }


    @Deprecated("Modification of 'controlFlowGraphReference' has no impact for FirConstructorBuilder", level = DeprecationLevel.HIDDEN)
    override var controlFlowGraphReference: FirControlFlowGraphReference?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildConstructor(init: FirConstructorBuilder.() -> Unit): FirConstructor {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirConstructorBuilder().apply(init).build()
}

@OptIn(FirImplementationDetail::class)
fun buildConstructor(
    source: KtSourceElement? = null,
    resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR,
    moduleData: FirModuleData,
    origin: FirDeclarationOrigin,
    attributes: FirDeclarationAttributes = FirDeclarationAttributes(),
    typeParameters: MutableList<FirTypeParameterRef> = mutableListOf(),
    status: FirDeclarationStatus,
    isLocal: Boolean,
    returnTypeRef: FirTypeRef,
    receiverParameter: FirReceiverParameter? = null,
    deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider,
    containerSource: DeserializedContainerSource? = null,
    dispatchReceiverType: ConeSimpleKotlinType? = null,
    contextParameters: MutableList<FirValueParameter> = mutableListOf(),
    valueParameters: MutableList<FirValueParameter> = mutableListOf(),
    contractDescription: FirContractDescription? = null,
    annotations: MutableList<FirAnnotation> = mutableListOf(),
    symbol: FirConstructorSymbol,
    delegatedConstructor: FirDelegatedConstructorCall? = null,
    body: FirBlock? = null,
): FirConstructor {
    return FirConstructorImpl(
        source,
        resolvePhase,
        moduleData,
        origin,
        attributes,
        typeParameters,
        status,
        isLocal,
        returnTypeRef,
        receiverParameter,
        deprecationsProvider,
        containerSource,
        dispatchReceiverType,
        contextParameters.toMutableOrEmpty(),
        valueParameters,
        contractDescription,
        annotations.toMutableOrEmpty(),
        symbol,
        delegatedConstructor,
        body,
    )
}
