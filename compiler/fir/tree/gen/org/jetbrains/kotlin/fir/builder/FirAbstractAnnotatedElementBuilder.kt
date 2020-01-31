/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirAbstractAnnotatedElementBuilder {
    var source: FirSourceElement? = null
    val annotations: MutableList<FirAnnotationCall> = mutableListOf()

    fun build(): FirAnnotationContainer {
        return FirAbstractAnnotatedElement(
            source,
            annotations,
        )
    }

}

@UseExperimental(ExperimentalContracts::class)
inline fun buildAbstractAnnotatedElement(init: FirAbstractAnnotatedElementBuilder.() -> Unit = {}): FirAnnotationContainer {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirAbstractAnnotatedElementBuilder().apply(init).build()
}
