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

abstract class FirIntersectionTypeRef : FirTypeRefWithNullability() {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract override val isMarkedNullable: Boolean
    abstract val leftType: FirTypeRef
    abstract val rightType: FirTypeRef


    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract fun replaceLeftType(newLeftType: FirTypeRef)

    abstract fun replaceRightType(newRightType: FirTypeRef)
}

inline fun <D> FirIntersectionTypeRef.transformAnnotations(transformer: FirTransformer<D>, data: D): FirIntersectionTypeRef  = 
    apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirIntersectionTypeRef.transformLeftType(transformer: FirTransformer<D>, data: D): FirIntersectionTypeRef  = 
    apply { replaceLeftType(leftType.transform(transformer, data)) }

inline fun <D> FirIntersectionTypeRef.transformRightType(transformer: FirTransformer<D>, data: D): FirIntersectionTypeRef  = 
    apply { replaceRightType(rightType.transform(transformer, data)) }
