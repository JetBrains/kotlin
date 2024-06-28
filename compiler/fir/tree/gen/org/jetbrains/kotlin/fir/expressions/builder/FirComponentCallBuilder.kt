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
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirComponentCallImpl
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeProjection

@FirBuilderDsl
class FirComponentCallBuilder : FirCallBuilder, FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var coneTypeOrNull: ConeKotlinType? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    val contextReceiverArguments: MutableList<FirExpression> = mutableListOf()
    val typeArguments: MutableList<FirTypeProjection> = mutableListOf()
    var dispatchReceiver: FirExpression? = null
    var extensionReceiver: FirExpression? = null
    override var source: KtSourceElement? = null
    val nonFatalDiagnostics: MutableList<ConeDiagnostic> = mutableListOf()
    override var argumentList: FirArgumentList = FirEmptyArgumentList
    lateinit var explicitReceiver: FirExpression
    var componentIndex: Int by kotlin.properties.Delegates.notNull<Int>()

    override fun build(): FirComponentCall {
        return FirComponentCallImpl(
            coneTypeOrNull,
            annotations.toMutableOrEmpty(),
            contextReceiverArguments.toMutableOrEmpty(),
            typeArguments.toMutableOrEmpty(),
            dispatchReceiver,
            extensionReceiver,
            source,
            nonFatalDiagnostics.toMutableOrEmpty(),
            argumentList,
            explicitReceiver,
            componentIndex,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildComponentCall(init: FirComponentCallBuilder.() -> Unit): FirComponentCall {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirComponentCallBuilder().apply(init).build()
}
