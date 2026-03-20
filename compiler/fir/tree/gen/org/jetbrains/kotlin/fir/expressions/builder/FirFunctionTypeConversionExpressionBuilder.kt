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
import org.jetbrains.kotlin.fir.expressions.FirFunctionTypeConversionExpression
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.expressions.impl.FirFunctionTypeConversionExpressionImpl
import org.jetbrains.kotlin.fir.types.ConeKotlinType

@FirBuilderDsl
class FirFunctionTypeConversionExpressionBuilder : FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: KtSourceElement? = null
    override var coneTypeOrNull: ConeKotlinType? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    lateinit var expression: FirExpression
    var usesFunctionKindConversion: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    override fun build(): FirFunctionTypeConversionExpression {
        return FirFunctionTypeConversionExpressionImpl(
            source,
            coneTypeOrNull,
            annotations.toMutableOrEmpty(),
            expression,
            usesFunctionKindConversion,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildFunctionTypeConversionExpression(init: FirFunctionTypeConversionExpressionBuilder.() -> Unit): FirFunctionTypeConversionExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirFunctionTypeConversionExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class, UnresolvedExpressionTypeAccess::class)
inline fun buildFunctionTypeConversionExpressionCopy(original: FirFunctionTypeConversionExpression, init: FirFunctionTypeConversionExpressionBuilder.() -> Unit): FirFunctionTypeConversionExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirFunctionTypeConversionExpressionBuilder()
    copyBuilder.source = original.source
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.expression = original.expression
    copyBuilder.usesFunctionKindConversion = original.usesFunctionKindConversion
    return copyBuilder.apply(init).build()
}
