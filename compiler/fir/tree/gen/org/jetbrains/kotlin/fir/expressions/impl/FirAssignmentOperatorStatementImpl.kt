/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.MutableOrEmptyList
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAssignmentOperatorStatement
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace

internal class FirAssignmentOperatorStatementImpl(
    override val source: KtSourceElement?,
    override var annotations: MutableOrEmptyList<FirAnnotation>,
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
        leftArgument = leftArgument.transform(transformer, data)
        return this
    }

    override fun <D> transformRightArgument(transformer: FirTransformer<D>, data: D): FirAssignmentOperatorStatementImpl {
        rightArgument = rightArgument.transform(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }
}
