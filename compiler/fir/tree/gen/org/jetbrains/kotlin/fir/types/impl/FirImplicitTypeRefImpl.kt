/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirImplicitTypeRefImpl(
    override val source: FirSourceElement?
) : FirImplicitTypeRef() {
    override val annotations: List<FirAnnotationCall> get() = emptyList()

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirImplicitTypeRefImpl {
        return this
    }
}
