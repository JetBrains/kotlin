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
import org.jetbrains.kotlin.fir.types.FirIntersectionTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirIntersectionTypeRefImpl

@FirBuilderDsl
class FirIntersectionTypeRefBuilder : FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    var isMarkedNullable: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    lateinit var leftType: FirTypeRef
    lateinit var rightType: FirTypeRef

    override fun build(): FirIntersectionTypeRef {
        return FirIntersectionTypeRefImpl(
            source,
            annotations.toMutableOrEmpty(),
            isMarkedNullable,
            leftType,
            rightType,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildIntersectionTypeRef(init: FirIntersectionTypeRefBuilder.() -> Unit): FirIntersectionTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirIntersectionTypeRefBuilder().apply(init).build()
}
