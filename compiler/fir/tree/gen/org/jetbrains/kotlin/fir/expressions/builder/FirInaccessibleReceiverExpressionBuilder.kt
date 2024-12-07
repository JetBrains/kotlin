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
import org.jetbrains.kotlin.fir.expressions.FirInaccessibleReceiverExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirInaccessibleReceiverExpressionImpl
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.types.ConeKotlinType

@FirBuilderDsl
class FirInaccessibleReceiverExpressionBuilder : FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: KtSourceElement? = null
    override var coneTypeOrNull: ConeKotlinType? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    lateinit var calleeReference: FirThisReference

    override fun build(): FirInaccessibleReceiverExpression {
        return FirInaccessibleReceiverExpressionImpl(
            source,
            coneTypeOrNull,
            annotations.toMutableOrEmpty(),
            calleeReference,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildInaccessibleReceiverExpression(init: FirInaccessibleReceiverExpressionBuilder.() -> Unit): FirInaccessibleReceiverExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirInaccessibleReceiverExpressionBuilder().apply(init).build()
}
