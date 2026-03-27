/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.builder

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirErrorTypeRefImpl

@FirBuilderDsl
class FirErrorTypeRefBuilder : FirAnnotationContainerBuilder {
    var source: KtSourceElement? = null
    override var annotations: MutableList<FirAnnotation> = mutableListOf()
    var coneType: ConeKotlinType? = null
    var delegatedTypeRef: FirTypeRef? = null
    var partiallyResolvedTypeRef: FirTypeRef? = null
    lateinit var diagnostic: ConeDiagnostic

    override fun build(): FirErrorTypeRef {
        return FirErrorTypeRefImpl(
            source,
            annotations.toMutableOrEmpty(),
            this.coneType,
            delegatedTypeRef,
            diagnostic,
            partiallyResolvedTypeRef = partiallyResolvedTypeRef,
        )
    }
}

fun buildErrorTypeRef(
    source: KtSourceElement? = null,
    annotations: MutableList<FirAnnotation> = mutableListOf(),
    coneType: ConeKotlinType? = null,
    delegatedTypeRef: FirTypeRef? = null,
    partiallyResolvedTypeRef: FirTypeRef? = null,
    diagnostic: ConeDiagnostic,
): FirErrorTypeRef {
    return FirErrorTypeRefImpl(
        source,
        annotations.toMutableOrEmpty(),
        coneType,
        delegatedTypeRef,
        diagnostic,
        partiallyResolvedTypeRef = partiallyResolvedTypeRef,
    )
}

fun buildErrorTypeRefCopy(
    original: FirErrorTypeRef,
    source: KtSourceElement? = original.source,
    annotations: MutableList<FirAnnotation> = original.annotations.toMutableList(),
    coneType: ConeKotlinType? = original.coneType,
    delegatedTypeRef: FirTypeRef? = original.delegatedTypeRef,
    partiallyResolvedTypeRef: FirTypeRef? = original.partiallyResolvedTypeRef,
    diagnostic: ConeDiagnostic = original.diagnostic,
): FirErrorTypeRef {
    return FirErrorTypeRefImpl(
        source,
        annotations.toMutableOrEmpty(),
        coneType,
        delegatedTypeRef,
        diagnostic,
        partiallyResolvedTypeRef = partiallyResolvedTypeRef,
    )
}
