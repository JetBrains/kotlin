/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")
package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.types.SmartcastStability
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirSmartCastedTypeRef : FirResolvedTypeRef() {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract override val type: ConeKotlinType
    abstract override val delegatedTypeRef: FirTypeRef?
    abstract override val isFromStubType: Boolean
    abstract val typesFromSmartCast: Collection<ConeKotlinType>
    abstract val originalType: ConeKotlinType
    abstract val smartcastType: ConeKotlinType
    abstract val isStable: Boolean
    abstract val smartcastStability: SmartcastStability


    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceDelegatedTypeRef(newDelegatedTypeRef: FirTypeRef?)
}

inline fun <D> FirSmartCastedTypeRef.transformAnnotations(transformer: FirTransformer<D>, data: D): FirSmartCastedTypeRef  = 
    apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirSmartCastedTypeRef.transformDelegatedTypeRef(transformer: FirTransformer<D>, data: D): FirSmartCastedTypeRef  = 
    apply { replaceDelegatedTypeRef(delegatedTypeRef?.transform(transformer, data)) }
