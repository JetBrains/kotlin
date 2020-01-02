/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.impl

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirAbstractAnnotatedElement : FirAnnotationContainer {
    override val source: FirSourceElement?
    override val annotations: MutableList<FirAnnotationCall>
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirAbstractAnnotatedElement
}
