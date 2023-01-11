/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.jvm.specialization.annotations.Monomorphic

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirAssignmentOperatorStatement : FirPureAbstractElement(), FirStatement {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract val operation: FirOperation
    abstract val leftArgument: FirExpression
    abstract val rightArgument: FirExpression

    override fun <R, D, @Monomorphic VT : FirVisitor<R, D>> accept(visitor: VT, data: D): R = visitor.visitAssignmentOperatorStatement(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E: FirElement, D, @Monomorphic TT: FirTransformer<D>> transform(transformer: TT, data: D): E = 
        transformer.transformAssignmentOperatorStatement(this, data) as E

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirAssignmentOperatorStatement

    abstract fun <D> transformLeftArgument(transformer: FirTransformer<D>, data: D): FirAssignmentOperatorStatement

    abstract fun <D> transformRightArgument(transformer: FirTransformer<D>, data: D): FirAssignmentOperatorStatement
}
