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
import org.jetbrains.kotlin.fir.FirTarget
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirContinueExpression
import org.jetbrains.kotlin.fir.expressions.FirLoop
import org.jetbrains.kotlin.fir.expressions.impl.FirContinueExpressionImpl
import org.jetbrains.kotlin.fir.types.ConeKotlinType

@FirBuilderDsl
class FirContinueExpressionBuilder : FirLoopJumpBuilder, FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: KtSourceElement? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    override lateinit var target: FirTarget<FirLoop>

    override fun build(): FirContinueExpression {
        return FirContinueExpressionImpl(
            source,
            annotations.toMutableOrEmpty(),
            target,
        )
    }


    @Deprecated("Modification of 'coneTypeOrNull' has no impact for FirContinueExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override var coneTypeOrNull: ConeKotlinType?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildContinueExpression(init: FirContinueExpressionBuilder.() -> Unit): FirContinueExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirContinueExpressionBuilder().apply(init).build()
}
