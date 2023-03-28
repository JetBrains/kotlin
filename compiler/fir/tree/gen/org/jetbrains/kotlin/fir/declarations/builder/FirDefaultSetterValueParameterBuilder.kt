/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
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
import org.jetbrains.kotlin.fir.declarations.FirResolveState
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.ResolveStateAccess
import org.jetbrains.kotlin.fir.declarations.UnresolvedDeprecationProvider
import org.jetbrains.kotlin.fir.declarations.asResolveState
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultSetterValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
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
class FirDefaultSetterValueParameterBuilder : FirAnnotationContainerBuilder {
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
    var initializer: FirExpression? = null
    var delegate: FirExpression? = null
    var isVar: Boolean = false
    var isVal: Boolean = true
    var getter: FirPropertyAccessor? = null
    var setter: FirPropertyAccessor? = null
    var backingField: FirBackingField? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    lateinit var symbol: FirValueParameterSymbol
    var defaultValue: FirExpression? = null
    lateinit var containingFunctionSymbol: FirFunctionSymbol<*>
    var isCrossinline: Boolean = false
    var isNoinline: Boolean = false
    var isVararg: Boolean = false

    override fun build(): FirValueParameter {
        return FirDefaultSetterValueParameter(
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
            initializer,
            delegate,
            isVar,
            isVal,
            getter,
            setter,
            backingField,
            annotations.toMutableOrEmpty(),
            symbol,
            defaultValue,
            containingFunctionSymbol,
            isCrossinline,
            isNoinline,
            isVararg,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildDefaultSetterValueParameter(init: FirDefaultSetterValueParameterBuilder.() -> Unit): FirValueParameter {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirDefaultSetterValueParameterBuilder().apply(init).build()
}
