/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCatch
import org.jetbrains.kotlin.fir.expressions.impl.FirCatchImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirCatchBuilder {
    var source: FirSourceElement? = null
    lateinit var parameter: FirValueParameter
    lateinit var block: FirBlock

    fun build(): FirCatch {
        return FirCatchImpl(
            source,
            parameter,
            block,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildCatch(init: FirCatchBuilder.() -> Unit): FirCatch {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirCatchBuilder().apply(init).build()
}
