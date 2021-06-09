/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirAnonymousObjectExpressionImpl
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@FirBuilderDsl
class FirAnonymousObjectExpressionBuilder : FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: FirSourceElement? = null
    override var typeRef: FirTypeRef = FirImplicitTypeRefImpl(null)
    lateinit var anonymousObject: FirAnonymousObject

    override val annotations: MutableList<FirAnnotationCall>
        get() = error("Should not be called")

    override fun build(): FirAnonymousObjectExpression {
        return FirAnonymousObjectExpressionImpl(
            source,
            typeRef,
            anonymousObject,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildAnonymousObjectExpression(init: FirAnonymousObjectExpressionBuilder.() -> Unit): FirAnonymousObjectExpression {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirAnonymousObjectExpressionBuilder().apply(init).build()
}
