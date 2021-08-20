/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirCollectionLiteralEntrySingle
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirCollectionLiteralEntrySingleImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirCollectionLiteralEntrySingleBuilder {
    var source: FirSourceElement? = null
    lateinit var expression: FirExpression

    fun build(): FirCollectionLiteralEntrySingle {
        return FirCollectionLiteralEntrySingleImpl(
            source,
            expression,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildCollectionLiteralEntrySingle(init: FirCollectionLiteralEntrySingleBuilder.() -> Unit): FirCollectionLiteralEntrySingle {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirCollectionLiteralEntrySingleBuilder().apply(init).build()
}
