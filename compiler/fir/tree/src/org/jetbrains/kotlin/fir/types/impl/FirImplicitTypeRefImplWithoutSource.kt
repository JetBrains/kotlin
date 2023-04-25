/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

object FirImplicitTypeRefImplWithoutSource : FirImplicitTypeRef() {
    override val source: KtSourceElement? get() = null
    override val annotations: List<FirAnnotation> get() = emptyList()

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirImplicitTypeRef {
        return this
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        return this
    }
}
