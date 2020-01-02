/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirCheckNotNullCall : FirPureAbstractElement(), FirExpression, FirCall, FirResolvable {
    abstract override val source: FirSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotationCall>
    abstract override val arguments: List<FirExpression>
    abstract override val calleeReference: FirReference

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitCheckNotNullCall(this, data)

    abstract override fun <D> transformArguments(transformer: FirTransformer<D>, data: D): FirCheckNotNullCall

    abstract override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirCheckNotNullCall
}
