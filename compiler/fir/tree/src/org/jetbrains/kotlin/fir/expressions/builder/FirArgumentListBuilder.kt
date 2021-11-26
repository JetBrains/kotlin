/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirArgumentListImpl
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@FirBuilderDsl
class FirArgumentListBuilder {
    var source: KtSourceElement? = null
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
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirArgumentListBuilder().apply(init).build()
}
