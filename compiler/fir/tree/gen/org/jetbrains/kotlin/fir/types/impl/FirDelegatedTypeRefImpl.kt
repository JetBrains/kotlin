/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.types.FirDelegatedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirDelegatedTypeRefImpl(
    override var delegate: FirExpression?,
    override var typeRef: FirTypeRef
) : FirDelegatedTypeRef() {
    override val source: FirSourceElement? get() = typeRef.source
    override val annotations: List<FirAnnotationCall> get() = typeRef.annotations

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        delegate?.accept(visitor, data)
        typeRef.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirDelegatedTypeRefImpl {
        delegate = delegate?.transformSingle(transformer, data)
        typeRef = typeRef.transformSingle(transformer, data)
        return this
    }
}
