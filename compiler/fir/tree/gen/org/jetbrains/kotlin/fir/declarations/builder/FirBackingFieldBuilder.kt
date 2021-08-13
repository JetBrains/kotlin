/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.DeprecationsPerUseSite
import org.jetbrains.kotlin.fir.declarations.FirBackingField
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirBackingFieldImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
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
class FirBackingFieldBuilder : FirAnnotationContainerBuilder {
    override var source: FirSourceElement? = null
    lateinit var moduleData: FirModuleData
    var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    lateinit var origin: FirDeclarationOrigin
    var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    lateinit var returnTypeRef: FirTypeRef
    var receiverTypeRef: FirTypeRef? = null
    var deprecation: DeprecationsPerUseSite? = null
    var containerSource: DeserializedContainerSource? = null
    var dispatchReceiverType: ConeKotlinType? = null
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
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    val typeParameters: MutableList<FirTypeParameter> = mutableListOf()
    lateinit var status: FirDeclarationStatus

    @OptIn(FirImplementationDetail::class)
    override fun build(): FirBackingField {
        return FirBackingFieldImpl(
            source,
            moduleData,
            resolvePhase,
            origin,
            attributes,
            returnTypeRef,
            receiverTypeRef,
            deprecation,
            containerSource,
            dispatchReceiverType,
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
            annotations,
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
