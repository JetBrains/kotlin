/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirNamedArgumentExpression
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirNamedArgumentExpressionImpl(
    override val source: FirSourceElement?,
    override var expression: FirExpression,
    override val isSpread: Boolean,
    override val name: Name
) : FirNamedArgumentExpression(), FirAbstractAnnotatedElement {
    override val typeRef: FirTypeRef get() = expression.typeRef
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        expression.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirNamedArgumentExpressionImpl {
        annotations.transformInplace(transformer, data)
        expression = expression.transformSingle(transformer, data)
        return this
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {}
}
