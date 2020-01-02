/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirQualifiedAccessWithoutCallee : FirStatement {
    override val source: FirSourceElement?
    override val annotations: List<FirAnnotationCall>
    val safe: Boolean
    val typeArguments: List<FirTypeProjection>
    val explicitReceiver: FirExpression?
    val dispatchReceiver: FirExpression
    val extensionReceiver: FirExpression

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitQualifiedAccessWithoutCallee(this, data)

    fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirQualifiedAccessWithoutCallee

    fun <D> transformExplicitReceiver(transformer: FirTransformer<D>, data: D): FirQualifiedAccessWithoutCallee

    fun <D> transformDispatchReceiver(transformer: FirTransformer<D>, data: D): FirQualifiedAccessWithoutCallee

    fun <D> transformExtensionReceiver(transformer: FirTransformer<D>, data: D): FirQualifiedAccessWithoutCallee
}
