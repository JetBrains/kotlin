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
import org.jetbrains.kotlin.fir.declarations.DeprecationsPerUseSite
import org.jetbrains.kotlin.fir.declarations.FirBackingField
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
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
open class FirValueParameterBuilder : FirAnnotationContainerBuilder {
    override var source: FirSourceElement? = null
    open lateinit var moduleData: FirModuleData
    open var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    open lateinit var origin: FirDeclarationOrigin
    open var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    open lateinit var returnTypeRef: FirTypeRef
    open var deprecation: DeprecationsPerUseSite? = null
    open var containerSource: DeserializedContainerSource? = null
    open var dispatchReceiverType: ConeKotlinType? = null
    open lateinit var name: Name
    open var backingField: FirBackingField? = null
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    open lateinit var symbol: FirValueParameterSymbol
    open var defaultValue: FirExpression? = null
    open var isCrossinline: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    open var isNoinline: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    open var isVararg: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    override fun build(): FirValueParameter {
        return FirValueParameterImpl(
            source,
            moduleData,
            resolvePhase,
            origin,
            attributes,
            returnTypeRef,
            deprecation,
            containerSource,
            dispatchReceiverType,
            name,
            backingField,
            annotations,
            symbol,
            defaultValue,
            isCrossinline,
            isNoinline,
            isVararg,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildValueParameter(init: FirValueParameterBuilder.() -> Unit): FirValueParameter {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirValueParameterBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildValueParameterCopy(original: FirValueParameter, init: FirValueParameterBuilder.() -> Unit): FirValueParameter {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirValueParameterBuilder()
    copyBuilder.source = original.source
    copyBuilder.moduleData = original.moduleData
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.origin = original.origin
    copyBuilder.attributes = original.attributes.copy()
    copyBuilder.returnTypeRef = original.returnTypeRef
    copyBuilder.deprecation = original.deprecation
    copyBuilder.containerSource = original.containerSource
    copyBuilder.dispatchReceiverType = original.dispatchReceiverType
    copyBuilder.name = original.name
    copyBuilder.backingField = original.backingField
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.symbol = original.symbol
    copyBuilder.defaultValue = original.defaultValue
    copyBuilder.isCrossinline = original.isCrossinline
    copyBuilder.isNoinline = original.isNoinline
    copyBuilder.isVararg = original.isVararg
    return copyBuilder.apply(init).build()
}
