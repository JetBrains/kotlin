/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedDelegateExpression
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirWrappedDelegateExpressionImpl(
    override val source: FirSourceElement?,
    override var expression: FirExpression
) : FirWrappedDelegateExpression(), FirAbstractAnnotatedElement {
    override val typeRef: FirTypeRef get() = expression.typeRef
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override lateinit var delegateProvider: FirExpression

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        expression.accept(visitor, data)
        delegateProvider.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirWrappedDelegateExpressionImpl {
        annotations.transformInplace(transformer, data)
        expression = expression.transformSingle(transformer, data)
        delegateProvider = delegateProvider.transformSingle(transformer, data)
        return this
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {}
}
