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
import org.jetbrains.kotlin.fir.expressions.FirCopyFunCallExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.expressions.impl.FirCopyFunCallExpressionImpl
import org.jetbrains.kotlin.fir.types.ConeKotlinType

@FirBuilderDsl
class FirCopyFunCallExpressionBuilder : FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: KtSourceElement? = null
    override var coneTypeOrNull: ConeKotlinType? = null
    override val annotations: MutableList<FirAnnotation> = []
    lateinit var originalExpression: FirFunctionCall

    override fun build(): FirCopyFunCallExpression {
        return FirCopyFunCallExpressionImpl(
            source,
            coneTypeOrNull,
            annotations.toMutableOrEmpty(),
            originalExpression,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildCopyFunCallExpression(init: FirCopyFunCallExpressionBuilder.() -> Unit): FirCopyFunCallExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirCopyFunCallExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class, UnresolvedExpressionTypeAccess::class)
inline fun buildCopyFunCallExpressionCopy(original: FirCopyFunCallExpression, init: FirCopyFunCallExpressionBuilder.() -> Unit): FirCopyFunCallExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirCopyFunCallExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.originalExpression = original.originalExpression
    return copyBuilder.apply(init).build()
}
