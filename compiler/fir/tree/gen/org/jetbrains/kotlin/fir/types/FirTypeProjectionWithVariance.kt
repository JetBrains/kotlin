/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirTypeProjectionWithVariance : FirTypeProjection() {
    abstract override val source: KtSourceElement?
    abstract val typeRef: FirTypeRef
    abstract val variance: Variance


    abstract fun replaceTypeRef(newTypeRef: FirTypeRef)
}

inline fun <D> FirTypeProjectionWithVariance.transformTypeRef(transformer: FirTransformer<D>, data: D): FirTypeProjectionWithVariance 
     = apply { replaceTypeRef(typeRef.transform(transformer, data)) }
