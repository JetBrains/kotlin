/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirArrayOfCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirArrayOfCallImpl(
    override val source: FirSourceElement?
) : FirArrayOfCall(), FirCallWithArgumentList, FirAbstractAnnotatedElement {
    override var typeRef: FirTypeRef = FirImplicitTypeRefImpl(null)
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override val arguments: MutableList<FirExpression> = mutableListOf()

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        typeRef.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        arguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirArrayOfCallImpl {
        typeRef = typeRef.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
        transformArguments(transformer, data)
        return this
    }

    override fun <D> transformArguments(transformer: FirTransformer<D>, data: D): FirArrayOfCallImpl {
        arguments.transformInplace(transformer, data)
        return this
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }
}
