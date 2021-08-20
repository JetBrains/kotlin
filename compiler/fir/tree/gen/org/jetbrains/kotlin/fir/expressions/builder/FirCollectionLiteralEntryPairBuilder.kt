/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirCollectionLiteralEntryPair
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirCollectionLiteralEntryPairImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirCollectionLiteralEntryPairBuilder {
    var source: FirSourceElement? = null
    lateinit var key: FirExpression
    lateinit var value: FirExpression

    fun build(): FirCollectionLiteralEntryPair {
        return FirCollectionLiteralEntryPairImpl(
            source,
            key,
            value,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildCollectionLiteralEntryPair(init: FirCollectionLiteralEntryPairBuilder.() -> Unit): FirCollectionLiteralEntryPair {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirCollectionLiteralEntryPairBuilder().apply(init).build()
}
