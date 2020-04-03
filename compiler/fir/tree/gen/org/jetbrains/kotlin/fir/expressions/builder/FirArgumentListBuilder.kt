/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirArgumentListImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirArgumentListBuilder {
    var source: FirSourceElement? = null
    val arguments: MutableList<FirExpression> = mutableListOf()

    fun build(): FirArgumentList {
        return FirArgumentListImpl(
            source,
            arguments,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildArgumentList(init: FirArgumentListBuilder.() -> Unit = {}): FirArgumentList {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirArgumentListBuilder().apply(init).build()
}
