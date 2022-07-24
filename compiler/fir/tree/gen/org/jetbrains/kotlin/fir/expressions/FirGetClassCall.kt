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

abstract class FirGetClassCall : FirExpression(), FirCall {
    abstract override val source: KtSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotation>
    abstract override val argumentList: FirArgumentList
    abstract val argument: FirExpression


    abstract override fun replaceTypeRef(newTypeRef: FirTypeRef)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceArgumentList(newArgumentList: FirArgumentList)

    abstract fun replaceArgument(newArgument: FirExpression)
}

inline fun <D> FirGetClassCall.transformTypeRef(transformer: FirTransformer<D>, data: D): FirGetClassCall 
     = apply { replaceTypeRef(typeRef.transform(transformer, data)) }

inline fun <D> FirGetClassCall.transformAnnotations(transformer: FirTransformer<D>, data: D): FirGetClassCall 
     = apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirGetClassCall.transformArgumentList(transformer: FirTransformer<D>, data: D): FirGetClassCall 
     = apply { replaceArgumentList(argumentList.transform(transformer, data)) }

inline fun <D> FirGetClassCall.transformArgument(transformer: FirTransformer<D>, data: D): FirGetClassCall 
     = apply { replaceArgument(argument.transform(transformer, data)) }
