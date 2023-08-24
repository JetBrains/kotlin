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
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.FirExpressionBuilder
import org.jetbrains.kotlin.fir.expressions.builder.FirQualifiedAccessExpressionBuilder
import org.jetbrains.kotlin.fir.expressions.impl.FirCallableReferenceAccessImpl
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
class FirCallableReferenceAccessBuilder : FirQualifiedAccessExpressionBuilder, FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var coneTypeOrNull: ConeKotlinType? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    override val contextReceiverArguments: MutableList<FirExpression> = mutableListOf()
    override val typeArguments: MutableList<FirTypeProjection> = mutableListOf()
    override var explicitReceiver: FirExpression? = null
    override var dispatchReceiver: FirExpression = FirNoReceiverExpression
    override var extensionReceiver: FirExpression = FirNoReceiverExpression
    override var source: KtSourceElement? = null
    lateinit var calleeReference: FirNamedReference
    var hasQuestionMarkAtLHS: Boolean = false

    override fun build(): FirCallableReferenceAccess {
        return FirCallableReferenceAccessImpl(
            coneTypeOrNull,
            annotations.toMutableOrEmpty(),
            contextReceiverArguments.toMutableOrEmpty(),
            typeArguments.toMutableOrEmpty(),
            explicitReceiver,
            dispatchReceiver,
            extensionReceiver,
            source,
            calleeReference,
            hasQuestionMarkAtLHS,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildCallableReferenceAccess(init: FirCallableReferenceAccessBuilder.() -> Unit): FirCallableReferenceAccess {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirCallableReferenceAccessBuilder().apply(init).build()
}
