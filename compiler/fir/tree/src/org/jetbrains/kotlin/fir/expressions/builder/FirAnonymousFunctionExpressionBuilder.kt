/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.builder.FirExpressionBuilder
import org.jetbrains.kotlin.fir.expressions.impl.FirAnonymousFunctionExpressionImpl
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*

@FirBuilderDsl
class FirAnonymousFunctionExpressionBuilder : FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: FirSourceElement? = null
    lateinit var anonymousFunction: FirAnonymousFunction

    override fun build(): FirAnonymousFunctionExpression {
        return FirAnonymousFunctionExpressionImpl(
            source,
            anonymousFunction
        )
    }


    @Deprecated("Modification of 'typeRef' has no impact for FirAnonymousFunctionExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override var typeRef: FirTypeRef
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'annotations' has no impact for FirAnonymousFunctionExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override val annotations: MutableList<FirAnnotationCall>
        get() = throw IllegalStateException()
}

@OptIn(ExperimentalContracts::class)
inline fun buildAnonymousFunctionExpression(init: FirAnonymousFunctionExpressionBuilder.() -> Unit = {}): FirAnonymousFunctionExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirAnonymousFunctionExpressionBuilder().apply(init).build()
}
