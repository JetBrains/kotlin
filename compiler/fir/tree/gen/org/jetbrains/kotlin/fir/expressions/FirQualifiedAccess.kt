/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirQualifiedAccess : FirResolvable, FirStatement {
    override val source: FirSourceElement?
    override val calleeReference: FirReference
    override val annotations: List<FirAnnotationCall>
    val typeArguments: List<FirTypeProjection>
    val explicitReceiver: FirExpression?
    val dispatchReceiver: FirExpression
    val extensionReceiver: FirExpression

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitQualifiedAccess(this, data)

    override fun replaceCalleeReference(newCalleeReference: FirReference)

    fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>)

    fun replaceExplicitReceiver(newExplicitReceiver: FirExpression?)

    override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirQualifiedAccess

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirQualifiedAccess

    fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirQualifiedAccess

    fun <D> transformExplicitReceiver(transformer: FirTransformer<D>, data: D): FirQualifiedAccess

    fun <D> transformDispatchReceiver(transformer: FirTransformer<D>, data: D): FirQualifiedAccess

    fun <D> transformExtensionReceiver(transformer: FirTransformer<D>, data: D): FirQualifiedAccess
}
