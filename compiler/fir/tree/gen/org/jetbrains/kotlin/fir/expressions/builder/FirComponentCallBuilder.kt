/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirComponentCall
import org.jetbrains.kotlin.fir.expressions.FirEmptyArgumentList
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCallOrigin
import org.jetbrains.kotlin.fir.expressions.builder.FirCallBuilder
import org.jetbrains.kotlin.fir.expressions.builder.FirExpressionBuilder
import org.jetbrains.kotlin.fir.expressions.impl.FirComponentCallImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirComponentCallBuilder : FirCallBuilder, FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: KtSourceElement? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    val contextReceiverArguments: MutableList<FirExpression> = mutableListOf()
    val typeArguments: MutableList<FirTypeProjection> = mutableListOf()
    var dispatchReceiver: FirExpression = FirNoReceiverExpression
    var extensionReceiver: FirExpression = FirNoReceiverExpression
    override var argumentList: FirArgumentList = FirEmptyArgumentList
    lateinit var explicitReceiver: FirExpression
    var componentIndex: Int by kotlin.properties.Delegates.notNull<Int>()

    override fun build(): FirComponentCall {
        return FirComponentCallImpl(
            source,
            annotations,
            contextReceiverArguments,
            typeArguments,
            dispatchReceiver,
            extensionReceiver,
            argumentList,
            explicitReceiver,
            componentIndex,
        )
    }


    @Deprecated("Modification of 'typeRef' has no impact for FirComponentCallBuilder", level = DeprecationLevel.HIDDEN)
    override var typeRef: FirTypeRef
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildComponentCall(init: FirComponentCallBuilder.() -> Unit): FirComponentCall {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirComponentCallBuilder().apply(init).build()
}
