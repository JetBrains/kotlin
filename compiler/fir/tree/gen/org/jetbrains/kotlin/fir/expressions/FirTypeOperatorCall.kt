/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirTypeOperatorCall : FirExpression(), FirCall {
    abstract override val source: KtSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotation>
    abstract override val argumentList: FirArgumentList
    abstract val operation: FirOperation
    abstract val conversionTypeRef: FirTypeRef


    abstract override fun replaceTypeRef(newTypeRef: FirTypeRef)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceArgumentList(newArgumentList: FirArgumentList)

    abstract fun replaceConversionTypeRef(newConversionTypeRef: FirTypeRef)
}

inline fun <D> FirTypeOperatorCall.transformTypeRef(transformer: FirTransformer<D>, data: D): FirTypeOperatorCall 
     = apply { replaceTypeRef(typeRef.transform(transformer, data)) }

inline fun <D> FirTypeOperatorCall.transformAnnotations(transformer: FirTransformer<D>, data: D): FirTypeOperatorCall 
     = apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirTypeOperatorCall.transformArgumentList(transformer: FirTransformer<D>, data: D): FirTypeOperatorCall 
     = apply { replaceArgumentList(argumentList.transform(transformer, data)) }

inline fun <D> FirTypeOperatorCall.transformConversionTypeRef(transformer: FirTransformer<D>, data: D): FirTypeOperatorCall 
     = apply { replaceConversionTypeRef(conversionTypeRef.transform(transformer, data)) }
