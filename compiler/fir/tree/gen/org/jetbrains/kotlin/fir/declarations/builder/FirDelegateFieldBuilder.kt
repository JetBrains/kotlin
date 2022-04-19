/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.DeprecationsPerUseSite
import org.jetbrains.kotlin.fir.declarations.FirBackingField
import org.jetbrains.kotlin.fir.declarations.FirContextReceiver
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirDelegateField
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.impl.FirDelegateFieldImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
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
class FirDelegateFieldBuilder : FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    lateinit var moduleData: FirModuleData
    var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    lateinit var origin: FirDeclarationOrigin
    var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    val typeParameters: MutableList<FirTypeParameterRef> = mutableListOf()
    lateinit var returnTypeRef: FirTypeRef
    var receiverTypeRef: FirTypeRef? = null
    var deprecation: DeprecationsPerUseSite? = null
    var containerSource: DeserializedContainerSource? = null
    var dispatchReceiverType: ConeSimpleKotlinType? = null
    val contextReceivers: MutableList<FirContextReceiver> = mutableListOf()
    lateinit var name: Name
    var isVar: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var isVal: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var getter: FirPropertyAccessor? = null
    var setter: FirPropertyAccessor? = null
    var backingField: FirBackingField? = null
    var delegateField: FirDelegateField? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    lateinit var symbol: FirDelegateFieldSymbol
    lateinit var propertySymbol: FirPropertySymbol
    lateinit var initializer: FirExpression
    lateinit var status: FirDeclarationStatus

    @OptIn(FirImplementationDetail::class)
    override fun build(): FirDelegateField {
        return FirDelegateFieldImpl(
            source,
            moduleData,
            resolvePhase,
            origin,
            attributes,
            typeParameters,
            returnTypeRef,
            receiverTypeRef,
            deprecation,
            containerSource,
            dispatchReceiverType,
            contextReceivers,
            name,
            isVar,
            isVal,
            getter,
            setter,
            backingField,
            delegateField,
            annotations,
            symbol,
            propertySymbol,
            initializer,
            status,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildDelegateField(init: FirDelegateFieldBuilder.() -> Unit): FirDelegateField {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirDelegateFieldBuilder().apply(init).build()
}
