/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

internal class FirErrorTypeRefImpl(
    override val source: KtSourceElement?,
    override val type: ConeKotlinType,
    override var delegatedTypeRef: FirTypeRef?,
    override val diagnostic: ConeDiagnostic,
    override val isFromStubType: Boolean = false
) : FirErrorTypeRef() {
    constructor(source: KtSourceElement?, delegatedTypeRef: FirTypeRef?, diagnostic: ConeDiagnostic,
        isFromStubType: Boolean = false
    ) : this(
        source,
        ConeErrorType(diagnostic),
        delegatedTypeRef,
        diagnostic,
        isFromStubType
    )

    override val annotations: MutableList<FirAnnotation> = mutableListOf()

    override val elementKind: FirElementKind
        get() = FirElementKind.ErrorTypeRef

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceDelegatedTypeRef(newDelegatedTypeRef: FirTypeRef?) {
        this.delegatedTypeRef = newDelegatedTypeRef
    }
}
