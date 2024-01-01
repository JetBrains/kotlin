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
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.expressions.impl.FirThisReceiverExpressionImpl
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeProjection

@FirBuilderDsl
class FirThisReceiverExpressionBuilder : FirQualifiedAccessExpressionBuilder, FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var coneTypeOrNull: ConeKotlinType? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    override val contextReceiverArguments: MutableList<FirExpression> = mutableListOf()
    override val typeArguments: MutableList<FirTypeProjection> = mutableListOf()
    override var source: KtSourceElement? = null
    override val nonFatalDiagnostics: MutableList<ConeDiagnostic> = mutableListOf()
    lateinit var calleeReference: FirThisReference
    var isImplicit: Boolean = false

    override fun build(): FirThisReceiverExpression {
        return FirThisReceiverExpressionImpl(
            coneTypeOrNull,
            annotations.toMutableOrEmpty(),
            contextReceiverArguments.toMutableOrEmpty(),
            typeArguments.toMutableOrEmpty(),
            source,
            nonFatalDiagnostics.toMutableOrEmpty(),
            calleeReference,
            isImplicit,
        )
    }


    @Deprecated("Modification of 'explicitReceiver' has no impact for FirThisReceiverExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override var explicitReceiver: FirExpression?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'dispatchReceiver' has no impact for FirThisReceiverExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override var dispatchReceiver: FirExpression?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'extensionReceiver' has no impact for FirThisReceiverExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override var extensionReceiver: FirExpression?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildThisReceiverExpression(init: FirThisReceiverExpressionBuilder.() -> Unit): FirThisReceiverExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirThisReceiverExpressionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class, UnresolvedExpressionTypeAccess::class)
inline fun buildThisReceiverExpressionCopy(original: FirThisReceiverExpression, init: FirThisReceiverExpressionBuilder.() -> Unit): FirThisReceiverExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = FirThisReceiverExpressionBuilder()
    copyBuilder.coneTypeOrNull = original.coneTypeOrNull
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.contextReceiverArguments.addAll(original.contextReceiverArguments)
    copyBuilder.typeArguments.addAll(original.typeArguments)
    copyBuilder.source = original.source
    copyBuilder.nonFatalDiagnostics.addAll(original.nonFatalDiagnostics)
    copyBuilder.calleeReference = original.calleeReference
    copyBuilder.isImplicit = original.isImplicit
    return copyBuilder.apply(init).build()
}
