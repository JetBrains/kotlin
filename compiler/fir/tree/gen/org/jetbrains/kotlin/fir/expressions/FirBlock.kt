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

abstract class FirBlock : FirExpression() {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract val statements: List<FirStatement>
    abstract override val typeRef: FirTypeRef


    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract fun replaceStatements(newStatements: List<FirStatement>)

    abstract override fun replaceTypeRef(newTypeRef: FirTypeRef)
}

inline fun <D> FirBlock.transformAnnotations(transformer: FirTransformer<D>, data: D): FirBlock 
     = apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirBlock.transformStatements(transformer: FirTransformer<D>, data: D): FirBlock 
     = apply { replaceStatements(statements.transform(transformer, data)) }

inline fun <D> FirBlock.transformTypeRef(transformer: FirTransformer<D>, data: D): FirBlock 
     = apply { replaceTypeRef(typeRef.transform(transformer, data)) }
