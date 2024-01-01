/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

/**
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.qualifiedAccessExpression]
 */
abstract class FirQualifiedAccessExpression : FirExpression(), FirResolvable, FirContextReceiverArgumentListOwner {
    @UnresolvedExpressionTypeAccess
    abstract override val coneTypeOrNull: ConeKotlinType?
    abstract override val annotations: List<FirAnnotation>
    abstract override val calleeReference: FirReference
    abstract override val contextReceiverArguments: List<FirExpression>
    abstract val typeArguments: List<FirTypeProjection>
    abstract val explicitReceiver: FirExpression?
    abstract val dispatchReceiver: FirExpression?
    abstract val extensionReceiver: FirExpression?
    abstract override val source: KtSourceElement?
    abstract val nonFatalDiagnostics: List<ConeDiagnostic>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitQualifiedAccessExpression(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformQualifiedAccessExpression(this, data) as E

    abstract override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeKotlinType?)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceCalleeReference(newCalleeReference: FirReference)

    abstract override fun replaceContextReceiverArguments(newContextReceiverArguments: List<FirExpression>)

    abstract fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>)

    abstract fun replaceExplicitReceiver(newExplicitReceiver: FirExpression?)

    abstract fun replaceDispatchReceiver(newDispatchReceiver: FirExpression?)

    abstract fun replaceExtensionReceiver(newExtensionReceiver: FirExpression?)

    @FirImplementationDetail
    abstract fun replaceSource(newSource: KtSourceElement?)

    abstract fun replaceNonFatalDiagnostics(newNonFatalDiagnostics: List<ConeDiagnostic>)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirQualifiedAccessExpression

    abstract override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirQualifiedAccessExpression

    abstract fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirQualifiedAccessExpression

    abstract fun <D> transformExplicitReceiver(transformer: FirTransformer<D>, data: D): FirQualifiedAccessExpression
}
