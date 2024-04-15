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
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirCompoundExpression
import org.jetbrains.kotlin.fir.expressions.FirCompoundExpressionBranch
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirCompoundExpressionImpl
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.impl.FirStubReference
import org.jetbrains.kotlin.fir.types.ConeKotlinType

@FirBuilderDsl
class FirCompoundExpressionBuilder : FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: KtSourceElement? = null
    override var coneTypeOrNull: ConeKotlinType? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    var calleeReference: FirReference = FirStubReference
    val properties: MutableList<FirVariable> = mutableListOf()
    lateinit var condition: FirExpression
    val branches: MutableList<FirCompoundExpressionBranch> = mutableListOf()

    override fun build(): FirCompoundExpression {
        return FirCompoundExpressionImpl(
            source,
            coneTypeOrNull,
            annotations.toMutableOrEmpty(),
            calleeReference,
            properties,
            condition,
            branches,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildCompoundExpression(init: FirCompoundExpressionBuilder.() -> Unit): FirCompoundExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirCompoundExpressionBuilder().apply(init).build()
}
