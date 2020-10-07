/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.impl.FirEmptyContractDescription
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.builder.FirFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.impl.FirPropertyAccessorImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirPropertyAccessorBuilder : FirFunctionBuilder, FirAnnotationContainerBuilder {
    override var source: FirSourceElement? = null
    override lateinit var session: FirSession
    var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override lateinit var origin: FirDeclarationOrigin
    override var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    override lateinit var returnTypeRef: FirTypeRef
    override val valueParameters: MutableList<FirValueParameter> = mutableListOf()
    override var body: FirBlock? = null
    lateinit var status: FirDeclarationStatus
    var containerSource: DeserializedContainerSource? = null
    var dispatchReceiverType: ConeKotlinType? = null
    var contractDescription: FirContractDescription = FirEmptyContractDescription
    lateinit var symbol: FirPropertyAccessorSymbol
    var isGetter: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    val typeParameters: MutableList<FirTypeParameter> = mutableListOf()

    @OptIn(FirImplementationDetail::class)
    override fun build(): FirPropertyAccessor {
        return FirPropertyAccessorImpl(
            source,
            session,
            resolvePhase,
            origin,
            attributes,
            returnTypeRef,
            valueParameters,
            body,
            status,
            containerSource,
            dispatchReceiverType,
            contractDescription,
            symbol,
            isGetter,
            annotations,
            typeParameters,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildPropertyAccessor(init: FirPropertyAccessorBuilder.() -> Unit): FirPropertyAccessor {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirPropertyAccessorBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildPropertyAccessorCopy(original: FirPropertyAccessor, init: FirPropertyAccessorBuilder.() -> Unit): FirPropertyAccessor {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirPropertyAccessorBuilder()
    copyBuilder.source = original.source
    copyBuilder.session = original.session
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.origin = original.origin
    copyBuilder.attributes = original.attributes
    copyBuilder.returnTypeRef = original.returnTypeRef
    copyBuilder.valueParameters.addAll(original.valueParameters)
    copyBuilder.body = original.body
    copyBuilder.status = original.status
    copyBuilder.containerSource = original.containerSource
    copyBuilder.dispatchReceiverType = original.dispatchReceiverType
    copyBuilder.contractDescription = original.contractDescription
    copyBuilder.symbol = original.symbol
    copyBuilder.isGetter = original.isGetter
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.typeParameters.addAll(original.typeParameters)
    return copyBuilder.apply(init).build()
}
