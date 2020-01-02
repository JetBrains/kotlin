/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBinaryLogicExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.LogicOperationKind
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirBinaryLogicExpressionImpl(
    override val source: FirSourceElement?,
    override var leftOperand: FirExpression,
    override var rightOperand: FirExpression,
    override val kind: LogicOperationKind
) : FirBinaryLogicExpression(), FirAbstractAnnotatedElement {
    override var typeRef: FirTypeRef = FirImplicitTypeRefImpl(null)
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        typeRef.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        leftOperand.accept(visitor, data)
        rightOperand.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirBinaryLogicExpressionImpl {
        transformLeftOperand(transformer, data)
        transformRightOperand(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformLeftOperand(transformer: FirTransformer<D>, data: D): FirBinaryLogicExpressionImpl {
        leftOperand = leftOperand.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformRightOperand(transformer: FirTransformer<D>, data: D): FirBinaryLogicExpressionImpl {
        rightOperand = rightOperand.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirBinaryLogicExpressionImpl {
        typeRef = typeRef.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }
}
