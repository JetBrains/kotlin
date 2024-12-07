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
import org.jetbrains.kotlin.fir.expressions.FirIncrementDecrementExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirIncrementDecrementExpressionImpl
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.Name

@FirBuilderDsl
class FirIncrementDecrementExpressionBuilder : FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: KtSourceElement? = null
    override var coneTypeOrNull: ConeKotlinType? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    var isPrefix: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    lateinit var operationName: Name
    lateinit var expression: FirExpression
    var operationSource: KtSourceElement? = null

    override fun build(): FirIncrementDecrementExpression {
        return FirIncrementDecrementExpressionImpl(
            source,
            coneTypeOrNull,
            annotations.toMutableOrEmpty(),
            isPrefix,
            operationName,
            expression,
            operationSource,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildIncrementDecrementExpression(init: FirIncrementDecrementExpressionBuilder.() -> Unit): FirIncrementDecrementExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirIncrementDecrementExpressionBuilder().apply(init).build()
}
