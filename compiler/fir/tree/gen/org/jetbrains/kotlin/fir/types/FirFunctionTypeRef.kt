/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")
package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirFunctionTypeRef : FirTypeRefWithNullability() {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract override val isMarkedNullable: Boolean
    abstract val receiverTypeRef: FirTypeRef?
    abstract val valueParameters: List<FirValueParameter>
    abstract val returnTypeRef: FirTypeRef
    abstract val isSuspend: Boolean
    abstract val contextReceiverTypeRefs: List<FirTypeRef>


    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?)

    abstract fun replaceValueParameters(newValueParameters: List<FirValueParameter>)

    abstract fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef)

    abstract fun replaceContextReceiverTypeRefs(newContextReceiverTypeRefs: List<FirTypeRef>)
}

inline fun <D> FirFunctionTypeRef.transformAnnotations(transformer: FirTransformer<D>, data: D): FirFunctionTypeRef  = 
    apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirFunctionTypeRef.transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirFunctionTypeRef  = 
    apply { replaceReceiverTypeRef(receiverTypeRef?.transform(transformer, data)) }

inline fun <D> FirFunctionTypeRef.transformValueParameters(transformer: FirTransformer<D>, data: D): FirFunctionTypeRef  = 
    apply { replaceValueParameters(valueParameters.transform(transformer, data)) }

inline fun <D> FirFunctionTypeRef.transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirFunctionTypeRef  = 
    apply { replaceReturnTypeRef(returnTypeRef.transform(transformer, data)) }

inline fun <D> FirFunctionTypeRef.transformContextReceiverTypeRefs(transformer: FirTransformer<D>, data: D): FirFunctionTypeRef  = 
    apply { replaceContextReceiverTypeRefs(contextReceiverTypeRefs.transform(transformer, data)) }
