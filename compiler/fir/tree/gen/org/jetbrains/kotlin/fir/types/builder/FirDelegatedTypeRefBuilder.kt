/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.types.FirDelegatedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirDelegatedTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirDelegatedTypeRefBuilder {
    var delegate: FirExpression? = null
    lateinit var typeRef: FirTypeRef

    fun build(): FirDelegatedTypeRef {
        return FirDelegatedTypeRefImpl(
            delegate,
            typeRef,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildDelegatedTypeRef(init: FirDelegatedTypeRefBuilder.() -> Unit): FirDelegatedTypeRef {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirDelegatedTypeRefBuilder().apply(init).build()
}
