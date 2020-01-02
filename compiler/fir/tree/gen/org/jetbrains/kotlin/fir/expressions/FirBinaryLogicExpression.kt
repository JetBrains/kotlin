/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirBinaryLogicExpression : FirPureAbstractElement(), FirExpression {
    abstract override val source: FirSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotationCall>
    abstract val leftOperand: FirExpression
    abstract val rightOperand: FirExpression
    abstract val kind: LogicOperationKind

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitBinaryLogicExpression(this, data)

    abstract fun <D> transformLeftOperand(transformer: FirTransformer<D>, data: D): FirBinaryLogicExpression

    abstract fun <D> transformRightOperand(transformer: FirTransformer<D>, data: D): FirBinaryLogicExpression

    abstract fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirBinaryLogicExpression
}
