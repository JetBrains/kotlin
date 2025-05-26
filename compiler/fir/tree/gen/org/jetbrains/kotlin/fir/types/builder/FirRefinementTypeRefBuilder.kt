/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.types.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.types.FirRefinementTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirRefinementTypeRefImpl

@FirBuilderDsl
class FirRefinementTypeRefBuilder : FirAnnotationContainerBuilder {
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    lateinit var source: KtSourceElement
    var isMarkedNullable: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    lateinit var underlyingType: FirTypeRef
    lateinit var predicate: FirAnonymousFunctionExpression

    override fun build(): FirRefinementTypeRef {
        return FirRefinementTypeRefImpl(
            annotations.toMutableOrEmpty(),
            source,
            isMarkedNullable,
            underlyingType,
            predicate,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildRefinementTypeRef(init: FirRefinementTypeRefBuilder.() -> Unit): FirRefinementTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirRefinementTypeRefBuilder().apply(init).build()
}
