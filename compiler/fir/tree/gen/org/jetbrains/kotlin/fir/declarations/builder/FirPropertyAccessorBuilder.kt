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
import org.jetbrains.kotlin.fir.declarations.impl.FirPropertyAccessorImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

@FirBuilderDsl
class FirPropertyAccessorBuilder : FirFunctionBuilder, FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    override var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override lateinit var moduleData: FirModuleData
    override lateinit var origin: FirDeclarationOrigin
    override var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    override lateinit var status: FirDeclarationStatus
    override lateinit var returnTypeRef: FirTypeRef
    override var staticReceiverParameter: FirTypeRef? = null
    override var deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider
    override var dispatchReceiverType: ConeSimpleKotlinType? = null
    override val valueParameters: MutableList<FirValueParameter> = mutableListOf()
    override var body: FirBlock? = null
    var contractDescription: FirContractDescription? = null
    lateinit var symbol: FirPropertyAccessorSymbol
    lateinit var propertySymbol: FirPropertySymbol
    var isGetter: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    override val annotations: MutableList<FirAnnotation> = mutableListOf()

    @OptIn(FirImplementationDetail::class)
    override fun build(): FirPropertyAccessor {
        return FirPropertyAccessorImpl(
            source,
            resolvePhase,
            moduleData,
            origin,
            attributes,
            status,
            returnTypeRef,
            staticReceiverParameter,
            deprecationsProvider,
            dispatchReceiverType,
            valueParameters,
            body,
            contractDescription,
            symbol,
            propertySymbol,
            isGetter,
            annotations.toMutableOrEmpty(),
        )
    }


    @Deprecated("Modification of 'containerSource' has no impact for FirPropertyAccessorBuilder", level = DeprecationLevel.HIDDEN)
    override var containerSource: DeserializedContainerSource?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'contextParameters' has no impact for FirPropertyAccessorBuilder", level = DeprecationLevel.HIDDEN)
    override val contextParameters: MutableList<FirValueParameter> = mutableListOf()
}

@OptIn(ExperimentalContracts::class)
inline fun buildPropertyAccessor(init: FirPropertyAccessorBuilder.() -> Unit): FirPropertyAccessor {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirPropertyAccessorBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildPropertyAccessorCopy(original: FirPropertyAccessor, init: FirPropertyAccessorBuilder.() -> Unit): FirPropertyAccessor {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirPropertyAccessorBuilder()
    copyBuilder.source = original.source
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.moduleData = original.moduleData
    copyBuilder.origin = original.origin
    copyBuilder.attributes = original.attributes.copy()
    copyBuilder.status = original.status
    copyBuilder.returnTypeRef = original.returnTypeRef
    copyBuilder.staticReceiverParameter = original.staticReceiverParameter
    copyBuilder.deprecationsProvider = original.deprecationsProvider
    copyBuilder.dispatchReceiverType = original.dispatchReceiverType
    copyBuilder.valueParameters.addAll(original.valueParameters)
    copyBuilder.body = original.body
    copyBuilder.contractDescription = original.contractDescription
    copyBuilder.propertySymbol = original.propertySymbol
    copyBuilder.isGetter = original.isGetter
    copyBuilder.annotations.addAll(original.annotations)
    return copyBuilder.apply(init).build()
}
