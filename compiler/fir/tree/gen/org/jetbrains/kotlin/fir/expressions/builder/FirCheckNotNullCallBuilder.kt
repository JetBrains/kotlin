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
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirCheckNotNullCall
import org.jetbrains.kotlin.fir.expressions.impl.FirCheckNotNullCallImpl
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.impl.FirStubReference
import org.jetbrains.kotlin.fir.types.ConeKotlinType

@FirBuilderDsl
class FirCheckNotNullCallBuilder : FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: KtSourceElement? = null
    override var coneTypeOrNull: ConeKotlinType? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    lateinit var argumentList: FirArgumentList
    var calleeReference: FirReference = FirStubReference

    override fun build(): FirCheckNotNullCall {
        return FirCheckNotNullCallImpl(
            source,
            coneTypeOrNull,
            annotations.toMutableOrEmpty(),
            argumentList,
            calleeReference,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildCheckNotNullCall(init: FirCheckNotNullCallBuilder.() -> Unit): FirCheckNotNullCall {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirCheckNotNullCallBuilder().apply(init).build()
}
