/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirAnnotationArgumentMappingImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyAnnotationArgumentMapping
import org.jetbrains.kotlin.name.Name
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@FirBuilderDsl
class FirAnnotationArgumentMappingBuilder {
    var source: KtSourceElement? = null
    val mapping: MutableMap<Name, FirExpression> = mutableMapOf()

    fun build(): FirAnnotationArgumentMapping {
        if (source == null && mapping.isEmpty()) return FirEmptyAnnotationArgumentMapping
        return FirAnnotationArgumentMappingImpl(source, mapping)
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildAnnotationArgumentMapping(init: FirAnnotationArgumentMappingBuilder.() -> Unit = {}): FirAnnotationArgumentMapping {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirAnnotationArgumentMappingBuilder().apply(init).build()
}
