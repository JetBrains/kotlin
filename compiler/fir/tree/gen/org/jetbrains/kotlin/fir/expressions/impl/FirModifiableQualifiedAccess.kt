/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessWithoutCallee
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirModifiableQualifiedAccess : FirQualifiedAccessWithoutCallee, FirAbstractAnnotatedElement {
    override val source: FirSourceElement?
    override val annotations: MutableList<FirAnnotationCall>
    override var safe: Boolean
    override val typeArguments: MutableList<FirTypeProjection>
    override var explicitReceiver: FirExpression?
    override var dispatchReceiver: FirExpression
    override var extensionReceiver: FirExpression
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirModifiableQualifiedAccess

    override fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirModifiableQualifiedAccess

    override fun <D> transformExplicitReceiver(transformer: FirTransformer<D>, data: D): FirModifiableQualifiedAccess

    override fun <D> transformDispatchReceiver(transformer: FirTransformer<D>, data: D): FirModifiableQualifiedAccess

    override fun <D> transformExtensionReceiver(transformer: FirTransformer<D>, data: D): FirModifiableQualifiedAccess
}
