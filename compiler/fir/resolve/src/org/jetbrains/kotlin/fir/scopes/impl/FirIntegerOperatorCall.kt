/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.builder.FirCallBuilder
import org.jetbrains.kotlin.fir.expressions.builder.FirExpressionBuilder
import org.jetbrains.kotlin.fir.expressions.builder.FirQualifiedAccessBuilder
import org.jetbrains.kotlin.fir.expressions.impl.FirFunctionCallImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildImplicitTypeRef

@OptIn(FirImplementationDetail::class)
class FirIntegerOperatorCall @FirImplementationDetail constructor(
    source: FirSourceElement?,
    typeRef: FirTypeRef,
    annotations: MutableList<FirAnnotationCall>,
    safe: Boolean,
    typeArguments: MutableList<FirTypeProjection>,
    explicitReceiver: FirExpression?,
    dispatchReceiver: FirExpression,
    extensionReceiver: FirExpression,
    argumentList: FirArgumentList,
    calleeReference: FirNamedReference,
) : FirFunctionCallImpl(
    source,
    typeRef,
    annotations,
    safe,
    typeArguments,
    explicitReceiver,
    dispatchReceiver,
    extensionReceiver,
    argumentList,
    calleeReference,
)

@FirBuilderDsl
class FirIntegerOperatorCallBuilder : FirQualifiedAccessBuilder, FirCallBuilder, FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: FirSourceElement? = null
    override var typeRef: FirTypeRef = buildImplicitTypeRef()
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override var safe: Boolean = false
    override val typeArguments: MutableList<FirTypeProjection> = mutableListOf()
    override var explicitReceiver: FirExpression? = null
    override var dispatchReceiver: FirExpression = FirNoReceiverExpression
    override var extensionReceiver: FirExpression = FirNoReceiverExpression
    lateinit var calleeReference: FirNamedReference
    override lateinit var argumentList: FirArgumentList

    @OptIn(FirImplementationDetail::class)
    override fun build(): FirIntegerOperatorCall {
        return FirIntegerOperatorCall(
            source,
            typeRef,
            annotations,
            safe,
            typeArguments,
            explicitReceiver,
            dispatchReceiver,
            extensionReceiver,
            argumentList,
            calleeReference,
        )
    }

}

inline fun buildFunctionCall(init: FirIntegerOperatorCallBuilder.() -> Unit): FirIntegerOperatorCall {
    return FirIntegerOperatorCallBuilder().apply(init).build()
}