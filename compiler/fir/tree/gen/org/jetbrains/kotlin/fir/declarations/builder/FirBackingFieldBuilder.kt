/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.declarations.DeprecationsProvider
import org.jetbrains.kotlin.fir.declarations.FirBackingField
import org.jetbrains.kotlin.fir.declarations.FirContextReceiver
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.UnresolvedDeprecationProvider
import org.jetbrains.kotlin.fir.declarations.impl.FirBackingFieldImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirBackingFieldBuilder : FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    lateinit var moduleData: FirModuleData
    lateinit var origin: FirDeclarationOrigin
    var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    lateinit var returnTypeRef: FirTypeRef
    var receiverParameter: FirReceiverParameter? = null
    var deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider
    var containerSource: DeserializedContainerSource? = null
    var dispatchReceiverType: ConeSimpleKotlinType? = null
    val contextReceivers: MutableList<FirContextReceiver> = mutableListOf()
    lateinit var name: Name
    var delegate: FirExpression? = null
    var isVar: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var isVal: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var getter: FirPropertyAccessor? = null
    var setter: FirPropertyAccessor? = null
    var backingField: FirBackingField? = null
    lateinit var symbol: FirBackingFieldSymbol
    lateinit var propertySymbol: FirPropertySymbol
    var initializer: FirExpression? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    val typeParameters: MutableList<FirTypeParameter> = mutableListOf()
    lateinit var status: FirDeclarationStatus

    @OptIn(FirImplementationDetail::class)
    override fun build(): FirBackingField {
        return FirBackingFieldImpl(
            source,
            resolvePhase,
            moduleData,
            origin,
            attributes,
            returnTypeRef,
            receiverParameter,
            deprecationsProvider,
            containerSource,
            dispatchReceiverType,
            contextReceivers.toMutableOrEmpty(),
            name,
            delegate,
            isVar,
            isVal,
            getter,
            setter,
            backingField,
            symbol,
            propertySymbol,
            initializer,
            annotations.toMutableOrEmpty(),
            typeParameters,
            status,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildBackingField(init: FirBackingFieldBuilder.() -> Unit): FirBackingField {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirBackingFieldBuilder().apply(init).build()
}
