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
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirSamConversionExpression
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.expressions.impl.FirSamConversionExpressionImpl
import org.jetbrains.kotlin.fir.types.ConeKotlinType

@FirBuilderDsl
class FirSamConversionExpressionBuilder : FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: KtSourceElement? = null
    override var coneTypeOrNull: ConeKotlinType? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    lateinit var expression: FirExpression

    override fun build(): FirSamConversionExpression {
        return FirSamConversionExpressionImpl(
            source,
            coneTypeOrNull,
            annotations.toMutableOrEmpty(),
            expression,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildSamConversionExpression(init: FirSamConversionExpressionBuilder.() -> Unit): FirSamConversionExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirSamConversionExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class, UnresolvedExpressionTypeAccess::class)
inline fun buildSamConversionExpressionCopy(original: FirSamConversionExpression, init: FirSamConversionExpressionBuilder.() -> Unit): FirSamConversionExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirSamConversionExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.expression = original.expression
    return copyBuilder.apply(init).build()
}
