/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedFunctionTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirResolvedFunctionTypeRefImpl(
    override val source: FirSourceElement?,
    override val type: ConeKotlinType,
    override val isMarkedNullable: Boolean,
    override var receiverTypeRef: FirTypeRef?,
    override var returnTypeRef: FirTypeRef
) : FirResolvedFunctionTypeRef(), FirAbstractAnnotatedElement {
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override val delegatedTypeRef: FirTypeRef? get() = null
    override val valueParameters: MutableList<FirValueParameter> = mutableListOf()

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        receiverTypeRef?.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }
        returnTypeRef.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirResolvedFunctionTypeRefImpl {
        annotations.transformInplace(transformer, data)
        receiverTypeRef = receiverTypeRef?.transformSingle(transformer, data)
        valueParameters.transformInplace(transformer, data)
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        return this
    }
}
