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

abstract class FirVarargArgumentsExpression : FirExpression() {
    abstract override val source: KtSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotation>
    abstract val arguments: List<FirExpression>
    abstract val varargElementType: FirTypeRef


    abstract override fun replaceTypeRef(newTypeRef: FirTypeRef)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract fun replaceArguments(newArguments: List<FirExpression>)

    abstract fun replaceVarargElementType(newVarargElementType: FirTypeRef)
}

inline fun <D> FirVarargArgumentsExpression.transformTypeRef(transformer: FirTransformer<D>, data: D): FirVarargArgumentsExpression  = 
    apply { replaceTypeRef(typeRef.transform(transformer, data)) }

inline fun <D> FirVarargArgumentsExpression.transformAnnotations(transformer: FirTransformer<D>, data: D): FirVarargArgumentsExpression  = 
    apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirVarargArgumentsExpression.transformArguments(transformer: FirTransformer<D>, data: D): FirVarargArgumentsExpression  = 
    apply { replaceArguments(arguments.transform(transformer, data)) }

inline fun <D> FirVarargArgumentsExpression.transformVarargElementType(transformer: FirTransformer<D>, data: D): FirVarargArgumentsExpression  = 
    apply { replaceVarargElementType(varargElementType.transform(transformer, data)) }
