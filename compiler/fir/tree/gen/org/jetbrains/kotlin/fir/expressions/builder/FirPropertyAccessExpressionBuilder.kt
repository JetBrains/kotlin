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
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.builder.FirExpressionBuilder
import org.jetbrains.kotlin.fir.expressions.builder.FirQualifiedAccessBuilder
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirPropertyAccessExpressionImpl
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirPropertyAccessExpressionBuilder : FirQualifiedAccessBuilder, FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: KtSourceElement? = null
    override var typeRef: FirTypeRef = FirImplicitTypeRefImpl(null)
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    lateinit var calleeReference: FirReference
    override val contextReceiverArguments: MutableList<FirExpression> = mutableListOf()
    override val typeArguments: MutableList<FirTypeProjection> = mutableListOf()
    override var explicitReceiver: FirExpression? = null
    override var dispatchReceiver: FirExpression = FirNoReceiverExpression
    override var extensionReceiver: FirExpression = FirNoReceiverExpression
    val nonFatalDiagnostics: MutableList<ConeDiagnostic> = mutableListOf()

    @OptIn(FirImplementationDetail::class)
    override fun build(): FirPropertyAccessExpression {
        return FirPropertyAccessExpressionImpl(
            source,
            typeRef,
            annotations.toMutableOrEmpty(),
            calleeReference,
            contextReceiverArguments.toMutableOrEmpty(),
            typeArguments.toMutableOrEmpty(),
            explicitReceiver,
            dispatchReceiver,
            extensionReceiver,
            nonFatalDiagnostics.toMutableOrEmpty(),
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildPropertyAccessExpression(init: FirPropertyAccessExpressionBuilder.() -> Unit): FirPropertyAccessExpression {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirPropertyAccessExpressionBuilder().apply(init).build()
}
