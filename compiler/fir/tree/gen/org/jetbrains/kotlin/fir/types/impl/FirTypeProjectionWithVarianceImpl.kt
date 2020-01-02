/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirTypeProjectionWithVarianceImpl(
    override val source: FirSourceElement?,
    override var typeRef: FirTypeRef,
    override val variance: Variance
) : FirTypeProjectionWithVariance() {
    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        typeRef.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirTypeProjectionWithVarianceImpl {
        typeRef = typeRef.transformSingle(transformer, data)
        return this
    }
}
