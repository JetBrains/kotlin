/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLoop
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirAbstractLoop : FirLoop, FirAbstractAnnotatedElement {
    override val source: FirSourceElement?
    override val annotations: MutableList<FirAnnotationCall>
    override var block: FirBlock
    override var condition: FirExpression
    override var label: FirLabel?
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirAbstractLoop

    override fun <D> transformBlock(transformer: FirTransformer<D>, data: D): FirAbstractLoop

    override fun <D> transformCondition(transformer: FirTransformer<D>, data: D): FirAbstractLoop

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirAbstractLoop
}
