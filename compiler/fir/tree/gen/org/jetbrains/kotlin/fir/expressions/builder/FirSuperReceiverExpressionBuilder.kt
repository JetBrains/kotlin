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
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirSuperReceiverExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirSuperReceiverExpressionImpl
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeProjection

@FirBuilderDsl
class FirSuperReceiverExpressionBuilder : FirQualifiedAccessExpressionBuilder, FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var coneTypeOrNull: ConeKotlinType? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    override val typeArguments: MutableList<FirTypeProjection> = mutableListOf()
    override var dispatchReceiver: FirExpression? = null
    override var source: KtSourceElement? = null
    override val nonFatalDiagnostics: MutableList<ConeDiagnostic> = mutableListOf()
    lateinit var calleeReference: FirSuperReference

    override fun build(): FirSuperReceiverExpression {
        return FirSuperReceiverExpressionImpl(
            coneTypeOrNull,
            annotations.toMutableOrEmpty(),
            typeArguments.toMutableOrEmpty(),
            dispatchReceiver,
            source,
            nonFatalDiagnostics.toMutableOrEmpty(),
            calleeReference,
        )
    }


    @Deprecated("Modification of 'contextArguments' has no impact for FirSuperReceiverExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override val contextArguments: MutableList<FirExpression> = mutableListOf()

    @Deprecated("Modification of 'explicitReceiver' has no impact for FirSuperReceiverExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override var explicitReceiver: FirExpression?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'extensionReceiver' has no impact for FirSuperReceiverExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override var extensionReceiver: FirExpression?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildSuperReceiverExpression(init: FirSuperReceiverExpressionBuilder.() -> Unit): FirSuperReceiverExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirSuperReceiverExpressionBuilder().apply(init).build()
}
