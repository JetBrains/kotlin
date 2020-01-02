/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirDoWhileLoop : FirPureAbstractElement(), FirLoop {
    abstract override val source: FirSourceElement?
    abstract override val annotations: List<FirAnnotationCall>
    abstract override val block: FirBlock
    abstract override val condition: FirExpression
    abstract override val label: FirLabel?

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitDoWhileLoop(this, data)

    abstract override fun <D> transformBlock(transformer: FirTransformer<D>, data: D): FirDoWhileLoop

    abstract override fun <D> transformCondition(transformer: FirTransformer<D>, data: D): FirDoWhileLoop

    abstract override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirDoWhileLoop
}
