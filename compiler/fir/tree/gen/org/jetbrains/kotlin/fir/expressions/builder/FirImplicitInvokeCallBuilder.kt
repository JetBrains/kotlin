/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirEmptyArgumentList
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCallOrigin
import org.jetbrains.kotlin.fir.expressions.FirImplicitInvokeCall
import org.jetbrains.kotlin.fir.expressions.builder.FirAbstractFunctionCallBuilder
import org.jetbrains.kotlin.fir.expressions.builder.FirExpressionBuilder
import org.jetbrains.kotlin.fir.expressions.impl.FirImplicitInvokeCallImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
open class FirImplicitInvokeCallBuilder : FirAbstractFunctionCallBuilder, FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var coneTypeOrNull: ConeKotlinType? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    override val contextReceiverArguments: MutableList<FirExpression> = mutableListOf()
    override val typeArguments: MutableList<FirTypeProjection> = mutableListOf()
    override var explicitReceiver: FirExpression? = null
    override var dispatchReceiver: FirExpression = FirNoReceiverExpression
    override var extensionReceiver: FirExpression = FirNoReceiverExpression
    override var source: KtSourceElement? = null
    override var argumentList: FirArgumentList = FirEmptyArgumentList
    override lateinit var calleeReference: FirNamedReference

    override fun build(): FirImplicitInvokeCall {
        return FirImplicitInvokeCallImpl(
            coneTypeOrNull,
            annotations.toMutableOrEmpty(),
            contextReceiverArguments.toMutableOrEmpty(),
            typeArguments.toMutableOrEmpty(),
            explicitReceiver,
            dispatchReceiver,
            extensionReceiver,
            source,
            argumentList,
            calleeReference,
        )
    }


    @Deprecated("Modification of 'origin' has no impact for FirImplicitInvokeCallBuilder", level = DeprecationLevel.HIDDEN)
    override var origin: FirFunctionCallOrigin
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildImplicitInvokeCall(init: FirImplicitInvokeCallBuilder.() -> Unit): FirImplicitInvokeCall {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirImplicitInvokeCallBuilder().apply(init).build()
}
