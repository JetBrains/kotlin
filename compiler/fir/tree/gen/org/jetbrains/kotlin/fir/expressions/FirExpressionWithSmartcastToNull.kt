/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.types.SmartcastStability
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.fir.FirImplementationDetail

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirExpressionWithSmartcastToNull : FirExpressionWithSmartcast() {
    abstract override val source: FirSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotationCall>
    abstract override val calleeReference: FirReference
    abstract override val typeArguments: List<FirTypeProjection>
    abstract override val explicitReceiver: FirExpression?
    abstract override val dispatchReceiver: FirExpression
    abstract override val extensionReceiver: FirExpression
    abstract override val nonFatalDiagnostics: List<ConeDiagnostic>
    abstract override val isStable: Boolean
    abstract override val originalExpression: FirQualifiedAccessExpression
    abstract override val typesFromSmartCast: Collection<ConeKotlinType>
    abstract override val originalType: FirTypeRef
    abstract override val smartcastType: FirTypeRef
    abstract val smartcastTypeWithoutNullableNothing: FirTypeRef
    abstract override val smartcastStability: SmartcastStability

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitExpressionWithSmartcastToNull(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E: FirElement, D> transform(transformer: FirTransformer<D>, data: D): E = 
        transformer.transformExpressionWithSmartcastToNull(this, data) as E

    @FirImplementationDetail
    abstract override fun replaceSource(newSource: FirSourceElement?)

    abstract override fun replaceTypeRef(newTypeRef: FirTypeRef)

    abstract override fun replaceCalleeReference(newCalleeReference: FirReference)

    abstract override fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>)

    abstract override fun replaceExplicitReceiver(newExplicitReceiver: FirExpression?)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirExpressionWithSmartcastToNull

    abstract override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirExpressionWithSmartcastToNull

    abstract override fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirExpressionWithSmartcastToNull

    abstract override fun <D> transformExplicitReceiver(transformer: FirTransformer<D>, data: D): FirExpressionWithSmartcastToNull

    abstract override fun <D> transformDispatchReceiver(transformer: FirTransformer<D>, data: D): FirExpressionWithSmartcastToNull

    abstract override fun <D> transformExtensionReceiver(transformer: FirTransformer<D>, data: D): FirExpressionWithSmartcastToNull
}
