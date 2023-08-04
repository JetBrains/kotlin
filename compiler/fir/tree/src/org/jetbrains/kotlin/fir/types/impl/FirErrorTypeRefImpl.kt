/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.MutableOrEmptyList
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace

internal class FirErrorTypeRefImpl(
    override val source: KtSourceElement?,
    override val type: ConeKotlinType,
    override var delegatedTypeRef: FirTypeRef?,
    override val diagnostic: ConeDiagnostic,
    override var partiallyResolvedTypeRef: FirTypeRef? = null,
) : FirErrorTypeRef() {
    constructor(
        source: KtSourceElement?, delegatedTypeRef: FirTypeRef?, diagnostic: ConeDiagnostic,
        partiallyResolvedTypeRef: FirTypeRef? = null,
    ) : this(
        source,
        ConeErrorType(diagnostic),
        delegatedTypeRef,
        diagnostic,
        partiallyResolvedTypeRef,
    )

    override val annotations: MutableOrEmptyList<FirAnnotation> = MutableOrEmptyList.empty()

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        partiallyResolvedTypeRef?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirErrorTypeRefImpl {
        transformAnnotations(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        throw AssertionError("Replacing annotations in FirErrorTypeRefImpl is not supported")
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirErrorTypeRefImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformPartiallyResolvedTypeRef(transformer: FirTransformer<D>, data: D): FirErrorTypeRef {
        partiallyResolvedTypeRef = partiallyResolvedTypeRef?.transform(transformer, data)
        return this
    }
}
