/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")
package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirResolvedTypeRef : FirTypeRef() {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract val type: ConeKotlinType
    abstract val delegatedTypeRef: FirTypeRef?
    abstract val isFromStubType: Boolean


    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract fun replaceDelegatedTypeRef(newDelegatedTypeRef: FirTypeRef?)
}

inline fun <D> FirResolvedTypeRef.transformAnnotations(transformer: FirTransformer<D>, data: D): FirResolvedTypeRef  = 
    apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirResolvedTypeRef.transformDelegatedTypeRef(transformer: FirTransformer<D>, data: D): FirResolvedTypeRef  = 
    apply { replaceDelegatedTypeRef(delegatedTypeRef?.transform(transformer, data)) }
