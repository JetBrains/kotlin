/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirArrayLiteral
import org.jetbrains.kotlin.fir.expressions.impl.FirArrayLiteralImpl
import org.jetbrains.kotlin.fir.types.ConeKotlinType

@FirBuilderDsl
class FirArrayLiteralBuilder : FirCallBuilder, FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: KtSourceElement? = null
    override var coneTypeOrNull: ConeKotlinType? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    override lateinit var argumentList: FirArgumentList

    override fun build(): FirArrayLiteral {
        return FirArrayLiteralImpl(
            source,
            coneTypeOrNull,
            annotations.toMutableOrEmpty(),
            argumentList,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildArrayLiteral(init: FirArrayLiteralBuilder.() -> Unit): FirArrayLiteral {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirArrayLiteralBuilder().apply(init).build()
}
