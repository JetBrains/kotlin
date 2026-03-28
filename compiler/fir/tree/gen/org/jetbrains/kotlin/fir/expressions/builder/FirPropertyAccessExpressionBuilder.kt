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
import org.jetbrains.kotlin.fir.FirIdeOnly
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.expressions.impl.FirPropertyAccessExpressionImpl
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeProjection

@FirBuilderDsl
class FirPropertyAccessExpressionBuilder : FirQualifiedAccessExpressionBuilder, FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var coneTypeOrNull: ConeKotlinType? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    override val contextArguments: MutableList<FirExpression> = mutableListOf()
    override val typeArguments: MutableList<FirTypeProjection> = mutableListOf()
    override var explicitReceiver: FirExpression? = null
    override var dispatchReceiver: FirExpression? = null
    override var extensionReceiver: FirExpression? = null
    override var source: KtSourceElement? = null
    override val nonFatalDiagnostics: MutableList<ConeDiagnostic> = mutableListOf()
    var contextSensitiveAlternative: FirPropertyAccessExpression? = null
    lateinit var calleeReference: FirNamedReference

    @OptIn(FirImplementationDetail::class)
    override fun build(): FirPropertyAccessExpression {
        return FirPropertyAccessExpressionImpl(
            coneTypeOrNull,
            annotations.toMutableOrEmpty(),
            contextArguments.toMutableOrEmpty(),
            typeArguments.toMutableOrEmpty(),
            explicitReceiver,
            dispatchReceiver,
            extensionReceiver,
            source,
            nonFatalDiagnostics.toMutableOrEmpty(),
            contextSensitiveAlternative,
            calleeReference,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildPropertyAccessExpression(init: FirPropertyAccessExpressionBuilder.() -> Unit): FirPropertyAccessExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirPropertyAccessExpressionBuilder().apply(init).build()
}

@OptIn(FirImplementationDetail::class)
fun buildPropertyAccessExpression(
    coneTypeOrNull: ConeKotlinType? = null,
    annotations: MutableList<FirAnnotation> = mutableListOf(),
    contextArguments: MutableList<FirExpression> = mutableListOf(),
    typeArguments: MutableList<FirTypeProjection> = mutableListOf(),
    explicitReceiver: FirExpression? = null,
    dispatchReceiver: FirExpression? = null,
    extensionReceiver: FirExpression? = null,
    source: KtSourceElement? = null,
    nonFatalDiagnostics: MutableList<ConeDiagnostic> = mutableListOf(),
    contextSensitiveAlternative: FirPropertyAccessExpression? = null,
    calleeReference: FirNamedReference,
): FirPropertyAccessExpression {
    return FirPropertyAccessExpressionImpl(
        coneTypeOrNull,
        annotations.toMutableOrEmpty(),
        contextArguments.toMutableOrEmpty(),
        typeArguments.toMutableOrEmpty(),
        explicitReceiver,
        dispatchReceiver,
        extensionReceiver,
        source,
        nonFatalDiagnostics.toMutableOrEmpty(),
        contextSensitiveAlternative,
        calleeReference,
    )
}

@OptIn(FirImplementationDetail::class, UnresolvedExpressionTypeAccess::class, FirIdeOnly::class)
fun buildPropertyAccessExpressionCopy(
    original: FirPropertyAccessExpression,
    coneTypeOrNull: ConeKotlinType? = original.coneTypeOrNull,
    annotations: MutableList<FirAnnotation> = original.annotations.toMutableList(),
    contextArguments: MutableList<FirExpression> = original.contextArguments.toMutableList(),
    typeArguments: MutableList<FirTypeProjection> = original.typeArguments.toMutableList(),
    explicitReceiver: FirExpression? = original.explicitReceiver,
    dispatchReceiver: FirExpression? = original.dispatchReceiver,
    extensionReceiver: FirExpression? = original.extensionReceiver,
    source: KtSourceElement? = original.source,
    nonFatalDiagnostics: MutableList<ConeDiagnostic> = original.nonFatalDiagnostics.toMutableList(),
    contextSensitiveAlternative: FirPropertyAccessExpression? = original.contextSensitiveAlternative,
    calleeReference: FirNamedReference = original.calleeReference,
): FirPropertyAccessExpression {
    return FirPropertyAccessExpressionImpl(
        coneTypeOrNull,
        annotations.toMutableOrEmpty(),
        contextArguments.toMutableOrEmpty(),
        typeArguments.toMutableOrEmpty(),
        explicitReceiver,
        dispatchReceiver,
        extensionReceiver,
        source,
        nonFatalDiagnostics.toMutableOrEmpty(),
        contextSensitiveAlternative,
        calleeReference,
    )
}
