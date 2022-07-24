/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirCheckNotNullCall : FirExpression(), FirCall, FirResolvable {
    abstract override val source: KtSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotation>
    abstract override val argumentList: FirArgumentList
    abstract override val calleeReference: FirReference


    abstract override fun replaceTypeRef(newTypeRef: FirTypeRef)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceArgumentList(newArgumentList: FirArgumentList)

    abstract override fun replaceCalleeReference(newCalleeReference: FirReference)
}

inline fun <D> FirCheckNotNullCall.transformTypeRef(transformer: FirTransformer<D>, data: D): FirCheckNotNullCall 
     = apply { replaceTypeRef(typeRef.transform(transformer, data)) }

inline fun <D> FirCheckNotNullCall.transformAnnotations(transformer: FirTransformer<D>, data: D): FirCheckNotNullCall 
     = apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirCheckNotNullCall.transformArgumentList(transformer: FirTransformer<D>, data: D): FirCheckNotNullCall 
     = apply { replaceArgumentList(argumentList.transform(transformer, data)) }

inline fun <D> FirCheckNotNullCall.transformCalleeReference(transformer: FirTransformer<D>, data: D): FirCheckNotNullCall 
     = apply { replaceCalleeReference(calleeReference.transform(transformer, data)) }
