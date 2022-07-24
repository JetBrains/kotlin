/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")
package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirStringConcatenationCall : FirCall, FirExpression() {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract override val argumentList: FirArgumentList
    abstract override val typeRef: FirTypeRef


    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceArgumentList(newArgumentList: FirArgumentList)

    abstract override fun replaceTypeRef(newTypeRef: FirTypeRef)
}

inline fun <D> FirStringConcatenationCall.transformAnnotations(transformer: FirTransformer<D>, data: D): FirStringConcatenationCall  = 
    apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirStringConcatenationCall.transformArgumentList(transformer: FirTransformer<D>, data: D): FirStringConcatenationCall  = 
    apply { replaceArgumentList(argumentList.transform(transformer, data)) }

inline fun <D> FirStringConcatenationCall.transformTypeRef(transformer: FirTransformer<D>, data: D): FirStringConcatenationCall  = 
    apply { replaceTypeRef(typeRef.transform(transformer, data)) }
