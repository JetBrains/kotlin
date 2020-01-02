/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.FirTarget
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirReturnExpression : FirPureAbstractElement(), FirJump<FirFunction<*>> {
    abstract override val source: FirSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotationCall>
    abstract override val target: FirTarget<FirFunction<*>>
    abstract val result: FirExpression

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitReturnExpression(this, data)

    abstract fun <D> transformResult(transformer: FirTransformer<D>, data: D): FirReturnExpression

    abstract fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirReturnExpression
}
