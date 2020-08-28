/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirAssignmentOperatorStatement
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirAssignmentOperatorStatementImpl(
    override val source: FirSourceElement?,
    override val annotations: MutableList<FirAnnotationCall>,
    override val operation: FirOperation,
    override var leftArgument: FirExpression,
    override var rightArgument: FirExpression,
) : FirAssignmentOperatorStatement() {
    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        leftArgument.accept(visitor, data)
        rightArgument.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirAssignmentOperatorStatementImpl {
        transformAnnotations(transformer, data)
        transformLeftArgument(transformer, data)
        transformRightArgument(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirAssignmentOperatorStatementImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformLeftArgument(transformer: FirTransformer<D>, data: D): FirAssignmentOperatorStatementImpl {
        leftArgument = leftArgument.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformRightArgument(transformer: FirTransformer<D>, data: D): FirAssignmentOperatorStatementImpl {
        rightArgument = rightArgument.transformSingle(transformer, data)
        return this
    }
}
