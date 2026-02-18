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
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirReplPropertyInitializer
import org.jetbrains.kotlin.fir.expressions.impl.FirReplPropertyInitializerImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

@FirBuilderDsl
class FirReplPropertyInitializerBuilder : FirAnnotationContainerBuilder {
    var source: KtSourceElement? = null
    lateinit var propertySymbol: FirPropertySymbol
    lateinit var initializer: FirExpression

    override fun build(): FirReplPropertyInitializer {
        return FirReplPropertyInitializerImpl(
            source,
            propertySymbol,
            initializer,
        )
    }


    @Deprecated("Modification of 'annotations' has no impact for FirReplPropertyInitializerBuilder", level = DeprecationLevel.HIDDEN)
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
}

@OptIn(ExperimentalContracts::class)
inline fun buildReplPropertyInitializer(init: FirReplPropertyInitializerBuilder.() -> Unit): FirReplPropertyInitializer {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirReplPropertyInitializerBuilder().apply(init).build()
}
