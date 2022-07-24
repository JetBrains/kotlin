/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirResolvedTypeRefImpl @FirImplementationDetail constructor(
    override val source: KtSourceElement?,
    override val annotations: MutableList<FirAnnotation>,
    override val type: ConeKotlinType,
    override var delegatedTypeRef: FirTypeRef?,
    override val isFromStubType: Boolean,
) : FirResolvedTypeRef() {
    override val elementKind get() = FirElementKind.ResolvedTypeRef

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceDelegatedTypeRef(newDelegatedTypeRef: FirTypeRef?) {
        delegatedTypeRef = newDelegatedTypeRef
    }
}
