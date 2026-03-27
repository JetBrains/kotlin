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
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirPropertyImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

@FirBuilderDsl
class FirPropertyBuilder : FirVariableBuilder, FirTypeParametersOwnerBuilder, FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    override var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override lateinit var moduleData: FirModuleData
    override lateinit var origin: FirDeclarationOrigin
    override var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    override lateinit var status: FirDeclarationStatus
    override var isLocal: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    override lateinit var returnTypeRef: FirTypeRef
    override var receiverParameter: FirReceiverParameter? = null
    override var deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider
    override var containerSource: DeserializedContainerSource? = null
    override var dispatchReceiverType: ConeSimpleKotlinType? = null
    override val contextParameters: MutableList<FirValueParameter> = mutableListOf()
    override lateinit var name: Name
    override var initializer: FirExpression? = null
    override var delegate: FirExpression? = null
    override var isVar: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    override var getter: FirPropertyAccessor? = null
    override var setter: FirPropertyAccessor? = null
    override var backingField: FirBackingField? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    lateinit var symbol: FirPropertySymbol
    var delegateFieldSymbol: FirDelegateFieldSymbol? = null
    var bodyResolveState: FirPropertyBodyResolveState = FirPropertyBodyResolveState.NOTHING_RESOLVED
    override val typeParameters: MutableList<FirTypeParameter> = mutableListOf()

    override fun build(): FirProperty {
        return FirPropertyImpl(
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
            name,
            initializer,
            delegate,
            isVar,
            getter,
            setter,
            backingField,
            annotations.toMutableOrEmpty(),
            symbol,
            delegateFieldSymbol,
            bodyResolveState,
            typeParameters,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildProperty(init: FirPropertyBuilder.() -> Unit): FirProperty {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirPropertyBuilder().apply(init).build()
}

@OptIn(FirImplementationDetail::class)
fun buildPropertyCopy(
    original: FirProperty,
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
    name: Name = original.name,
    initializer: FirExpression? = original.initializer,
    delegate: FirExpression? = original.delegate,
    isVar: Boolean = original.isVar,
    getter: FirPropertyAccessor? = original.getter,
    setter: FirPropertyAccessor? = original.setter,
    backingField: FirBackingField? = original.backingField,
    annotations: MutableList<FirAnnotation> = original.annotations.toMutableList(),
    symbol: FirPropertySymbol,
    delegateFieldSymbol: FirDelegateFieldSymbol? = original.delegateFieldSymbol,
    bodyResolveState: FirPropertyBodyResolveState = original.bodyResolveState,
    typeParameters: MutableList<FirTypeParameter> = original.typeParameters.toMutableList(),
): FirProperty {
    return FirPropertyImpl(
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
        name,
        initializer,
        delegate,
        isVar,
        getter,
        setter,
        backingField,
        annotations.toMutableOrEmpty(),
        symbol,
        delegateFieldSymbol,
        bodyResolveState,
        typeParameters,
    )
}
