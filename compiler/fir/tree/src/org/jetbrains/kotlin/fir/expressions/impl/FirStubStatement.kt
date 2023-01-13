/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

object FirStubStatement : FirPureAbstractElement(), FirStatement {
    override val source: KtSourceElement?
        get() = null

    override val annotations: List<FirAnnotation>
        get() = emptyList()

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        throw AssertionError("Mutating annotations of FirStubStatement is not supported")
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirStatement {
        return this
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        return this
    }
}
