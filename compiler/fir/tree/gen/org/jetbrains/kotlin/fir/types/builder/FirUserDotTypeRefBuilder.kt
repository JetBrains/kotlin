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
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirUserDotTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirUserDotTypeRefImpl
import org.jetbrains.kotlin.name.Name

@FirBuilderDsl
class FirUserDotTypeRefBuilder : FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    var isMarkedNullable: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    lateinit var name: Name
    val typeArguments: MutableList<FirTypeProjection> = mutableListOf()

    override fun build(): FirUserDotTypeRef {
        return FirUserDotTypeRefImpl(
            source,
            annotations.toMutableOrEmpty(),
            isMarkedNullable,
            name,
            typeArguments.toMutableOrEmpty(),
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildUserDotTypeRef(init: FirUserDotTypeRefBuilder.() -> Unit): FirUserDotTypeRef {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirUserDotTypeRefBuilder().apply(init).build()
}
