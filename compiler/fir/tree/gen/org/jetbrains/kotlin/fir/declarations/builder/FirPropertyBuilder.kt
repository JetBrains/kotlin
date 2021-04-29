/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.builder.FirDeclarationBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirTypeParametersOwnerBuilder
import org.jetbrains.kotlin.fir.declarations.impl.FirPropertyImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirPropertyBuilder : FirDeclarationBuilder, FirTypeParametersOwnerBuilder, FirAnnotationContainerBuilder {
    override var source: FirSourceElement? = null
    override lateinit var moduleData: FirModuleData
    override var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override lateinit var origin: FirDeclarationOrigin
    override var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    lateinit var returnTypeRef: FirTypeRef
    var receiverTypeRef: FirTypeRef? = null
    lateinit var name: Name
    var initializer: FirExpression? = null
    var delegate: FirExpression? = null
    var delegateFieldSymbol: FirDelegateFieldSymbol<FirProperty>? = null
    var isVar: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var getter: FirPropertyAccessor? = null
    var setter: FirPropertyAccessor? = null
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override val typeParameters: MutableList<FirTypeParameter> = mutableListOf()
    var containerSource: DeserializedContainerSource? = null
    var dispatchReceiverType: ConeKotlinType? = null
    lateinit var symbol: FirPropertySymbol
    var isLocal: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    lateinit var status: FirDeclarationStatus

    override fun build(): FirProperty {
        return FirPropertyImpl(
            source,
            moduleData,
            resolvePhase,
            origin,
            attributes,
            returnTypeRef,
            receiverTypeRef,
            name,
            initializer,
            delegate,
            delegateFieldSymbol,
            isVar,
            getter,
            setter,
            annotations,
            typeParameters,
            containerSource,
            dispatchReceiverType,
            symbol,
            isLocal,
            status,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildProperty(init: FirPropertyBuilder.() -> Unit): FirProperty {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirPropertyBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildPropertyCopy(original: FirProperty, init: FirPropertyBuilder.() -> Unit): FirProperty {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirPropertyBuilder()
    copyBuilder.source = original.source
    copyBuilder.moduleData = original.moduleData
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.origin = original.origin
    copyBuilder.attributes = original.attributes.copy()
    copyBuilder.returnTypeRef = original.returnTypeRef
    copyBuilder.receiverTypeRef = original.receiverTypeRef
    copyBuilder.name = original.name
    copyBuilder.initializer = original.initializer
    copyBuilder.delegate = original.delegate
    copyBuilder.delegateFieldSymbol = original.delegateFieldSymbol
    copyBuilder.isVar = original.isVar
    copyBuilder.getter = original.getter
    copyBuilder.setter = original.setter
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.typeParameters.addAll(original.typeParameters)
    copyBuilder.containerSource = original.containerSource
    copyBuilder.dispatchReceiverType = original.dispatchReceiverType
    copyBuilder.symbol = original.symbol
    copyBuilder.isLocal = original.isLocal
    copyBuilder.status = original.status
    return copyBuilder.apply(init).build()
}
