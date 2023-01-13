/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirFunctionTypeParameter
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
    abstract val parameters: List<FirFunctionTypeParameter>
    abstract val returnTypeRef: FirTypeRef
    abstract val isSuspend: Boolean
    abstract val contextReceiverTypeRefs: List<FirTypeRef>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitFunctionTypeRef(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformFunctionTypeRef(this, data) as E

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirFunctionTypeRef
}
