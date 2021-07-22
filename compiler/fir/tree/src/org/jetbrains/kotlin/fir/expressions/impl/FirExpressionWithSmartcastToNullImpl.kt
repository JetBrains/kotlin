/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirExpressionWithSmartcastToNull
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.types.SmartcastStability

class FirExpressionWithSmartcastToNullImpl(
    override var originalExpression: FirQualifiedAccessExpression,
    override val smartcastType: FirTypeRef,
    override val typesFromSmartCast: Collection<ConeKotlinType>,
    override val smartcastStability: SmartcastStability,
    override val smartcastTypeWithoutNullableNothing: FirTypeRef,
    override val nonFatalDiagnostics: List<ConeDiagnostic>
) : FirExpressionWithSmartcastToNull() {
    init {
        assert(originalExpression.typeRef is FirResolvedTypeRef)
    }

    override val source: FirSourceElement? get() = originalExpression.source
    override val annotations: List<FirAnnotationCall> get() = originalExpression.annotations
    override val typeArguments: List<FirTypeProjection> get() = originalExpression.typeArguments
    override val explicitReceiver: FirExpression? get() = originalExpression.explicitReceiver
    override val dispatchReceiver: FirExpression get() = originalExpression.dispatchReceiver
    override val extensionReceiver: FirExpression get() = originalExpression.extensionReceiver
    override val isStable: Boolean get() = smartcastStability == SmartcastStability.STABLE_VALUE
    override val calleeReference: FirReference get() = originalExpression.calleeReference
    override val originalType: FirTypeRef get() = originalExpression.typeRef

    // A FirExpressionWithSmartcast is only an effective smartcast if `smartcastStability == SmartcastStability.STABLE_VALUE`. Otherwise,
    // it's the same as the `originalExpression` under the hood. The reason we still create such a smartcast expression is for diagnostics
    // purpose only.
    override val typeRef: FirTypeRef get() = if (isStable) smartcastType else originalType

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirExpressionWithSmartcastToNull {
        originalExpression = originalExpression.transformSingle(transformer, data)
        return this
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        originalExpression.accept(visitor, data)
    }

    override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirExpressionWithSmartcastToNull {
        throw IllegalStateException()
    }

    override fun <D> transformExplicitReceiver(transformer: FirTransformer<D>, data: D): FirExpressionWithSmartcastToNull {
        throw IllegalStateException()
    }

    override fun <D> transformDispatchReceiver(transformer: FirTransformer<D>, data: D): FirExpressionWithSmartcastToNull {
        throw IllegalStateException()
    }

    override fun <D> transformExtensionReceiver(transformer: FirTransformer<D>, data: D): FirExpressionWithSmartcastToNull {
        throw IllegalStateException()
    }

    override fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirExpressionWithSmartcastToNull {
        throw IllegalStateException()
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirExpressionWithSmartcastToNull {
        throw IllegalStateException()
    }

    override fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>) {
        throw IllegalStateException()
    }

    override fun replaceCalleeReference(newCalleeReference: FirReference) {
        throw IllegalStateException()
    }

    override fun replaceExplicitReceiver(newExplicitReceiver: FirExpression?) {
        throw IllegalStateException()
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {}

    @FirImplementationDetail
    override fun replaceSource(newSource: FirSourceElement?) {
    }
}
