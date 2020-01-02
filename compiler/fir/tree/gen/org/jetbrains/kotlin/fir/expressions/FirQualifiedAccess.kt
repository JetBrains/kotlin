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

interface FirQualifiedAccess : FirQualifiedAccessWithoutCallee, FirResolvable {
    override val source: FirSourceElement?
    override val annotations: List<FirAnnotationCall>
    override val safe: Boolean
    override val typeArguments: List<FirTypeProjection>
    override val explicitReceiver: FirExpression?
    override val dispatchReceiver: FirExpression
    override val extensionReceiver: FirExpression
    override val calleeReference: FirReference

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitQualifiedAccess(this, data)

    override fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirQualifiedAccess

    override fun <D> transformExplicitReceiver(transformer: FirTransformer<D>, data: D): FirQualifiedAccess

    override fun <D> transformDispatchReceiver(transformer: FirTransformer<D>, data: D): FirQualifiedAccess

    override fun <D> transformExtensionReceiver(transformer: FirTransformer<D>, data: D): FirQualifiedAccess

    override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirQualifiedAccess
}
