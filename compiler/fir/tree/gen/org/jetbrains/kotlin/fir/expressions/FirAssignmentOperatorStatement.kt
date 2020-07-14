/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirAssignmentOperatorStatement : FirPureAbstractElement(), FirStatement {
    abstract override val source: FirSourceElement?
    abstract override val annotations: List<FirAnnotationCall>
    abstract val operation: FirOperation
    abstract val leftArgument: FirExpression
    abstract val rightArgument: FirExpression

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitAssignmentOperatorStatement(this, data)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirAssignmentOperatorStatement

    abstract fun <D> transformLeftArgument(transformer: FirTransformer<D>, data: D): FirAssignmentOperatorStatement

    abstract fun <D> transformRightArgument(transformer: FirTransformer<D>, data: D): FirAssignmentOperatorStatement
}
