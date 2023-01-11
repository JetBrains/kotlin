/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.jvm.specialization.annotations.Monomorphic

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirReceiverParameter : FirPureAbstractElement(), FirAnnotationContainer {
    abstract override val source: KtSourceElement?
    abstract val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotation>

    override fun <R, D, @Monomorphic VT : FirVisitor<R, D>> accept(visitor: VT, data: D): R = visitor.visitReceiverParameter(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E: FirElement, D, @Monomorphic TT: FirTransformer<D>> transform(transformer: TT, data: D): E = 
        transformer.transformReceiverParameter(this, data) as E

    abstract fun replaceTypeRef(newTypeRef: FirTypeRef)

    abstract fun <D> transformTypeRef(transformer: FirTransformer<D>, data: D): FirReceiverParameter

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirReceiverParameter
}
