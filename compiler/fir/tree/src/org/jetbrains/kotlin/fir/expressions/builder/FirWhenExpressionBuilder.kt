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
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirWhenExpressionImpl
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.impl.FirStubReference
import org.jetbrains.kotlin.fir.types.ConeKotlinType

@FirBuilderDsl
class FirWhenExpressionBuilder : FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: KtSourceElement? = null
    override var coneTypeOrNull: ConeKotlinType? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    var calleeReference: FirReference = FirStubReference
    var subject: FirExpression? = null
    var subjectVariable: FirVariable? = null
    val branches: MutableList<FirWhenBranch> = mutableListOf()
    var exhaustivenessStatus: ExhaustivenessStatus? = null
    var usedAsExpression: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    override fun build(): FirWhenExpression {
        return FirWhenExpressionImpl(
            source,
            coneTypeOrNull,
            annotations.toMutableOrEmpty(),
            calleeReference,
            subject,
            subjectVariable,
            branches,
            exhaustivenessStatus,
            usedAsExpression,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildWhenExpression(init: FirWhenExpressionBuilder.() -> Unit): FirWhenExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirWhenExpressionBuilder().apply(init).build()
}
