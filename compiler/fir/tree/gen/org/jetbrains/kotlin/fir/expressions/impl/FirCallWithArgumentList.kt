/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirCallWithArgumentList : FirCall, FirAbstractAnnotatedElement {
    override val source: FirSourceElement?
    override val annotations: MutableList<FirAnnotationCall>
    override val arguments: MutableList<FirExpression>
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirCallWithArgumentList

    override fun <D> transformArguments(transformer: FirTransformer<D>, data: D): FirCallWithArgumentList
}
