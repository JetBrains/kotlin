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
import org.jetbrains.kotlin.fir.FirExpressionRef
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirDelayedPropertyInitializer
import org.jetbrains.kotlin.fir.expressions.FirReplPropertyInitializer
import org.jetbrains.kotlin.fir.expressions.impl.FirDelayedPropertyInitializerImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType

@FirBuilderDsl
class FirDelayedPropertyInitializerBuilder : FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: KtSourceElement? = null
    lateinit var functionSymbol: FirNamedFunctionSymbol
    lateinit var expressionRef: FirExpressionRef<FirReplPropertyInitializer>

    override fun build(): FirDelayedPropertyInitializer {
        return FirDelayedPropertyInitializerImpl(
            source,
            functionSymbol,
            expressionRef,
        )
    }


    @Deprecated("Modification of 'coneTypeOrNull' has no impact for FirDelayedPropertyInitializerBuilder", level = DeprecationLevel.HIDDEN)
    override var coneTypeOrNull: ConeKotlinType?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'annotations' has no impact for FirDelayedPropertyInitializerBuilder", level = DeprecationLevel.HIDDEN)
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
}

@OptIn(ExperimentalContracts::class)
inline fun buildDelayedPropertyInitializer(init: FirDelayedPropertyInitializerBuilder.() -> Unit): FirDelayedPropertyInitializer {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirDelayedPropertyInitializerBuilder().apply(init).build()
}
