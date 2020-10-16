/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirLazyBlock @FirImplementationDetail constructor(
    override val source: FirSourceElement?,
) : FirBlock() {
    override val annotations: List<FirAnnotationCall> get() = error("FirLazyBlock should be calculated before accessing")
    override val statements: List<FirStatement> get() = error("FirLazyBlock should be calculated before accessing")
    override val typeRef: FirTypeRef get() = error("FirLazyBlock should be calculated before accessing")

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirLazyBlock {
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirLazyBlock {
        return this
    }

    override fun <D> transformStatements(transformer: FirTransformer<D>, data: D): FirLazyBlock {
        return this
    }

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirLazyBlock {
        return this
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {}
}
