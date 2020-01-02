/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirBlockImpl(
    override val source: FirSourceElement?
) : FirBlock(), FirAbstractAnnotatedElement {
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override val statements: MutableList<FirStatement> = mutableListOf()
    override var typeRef: FirTypeRef = FirImplicitTypeRefImpl(null)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        statements.forEach { it.accept(visitor, data) }
        typeRef.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirBlockImpl {
        annotations.transformInplace(transformer, data)
        statements.transformInplace(transformer, data)
        typeRef = typeRef.transformSingle(transformer, data)
        return this
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }
}
