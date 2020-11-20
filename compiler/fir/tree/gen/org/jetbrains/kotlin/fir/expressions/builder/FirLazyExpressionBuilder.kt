/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirLazyExpression
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirLazyExpressionBuilder : FirAnnotationContainerBuilder {
    override var source: FirSourceElement? = null

    @OptIn(FirImplementationDetail::class)
    override fun build(): FirExpression {
        return FirLazyExpression(
            source,
        )
    }


    @Deprecated("Modification of 'annotations' has no impact for FirLazyExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
}

@OptIn(ExperimentalContracts::class)
inline fun buildLazyExpression(init: FirLazyExpressionBuilder.() -> Unit = {}): FirExpression {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirLazyExpressionBuilder().apply(init).build()
}
