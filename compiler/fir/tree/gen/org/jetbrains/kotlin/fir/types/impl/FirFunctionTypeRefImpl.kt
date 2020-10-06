/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.types.FirFunctionTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirFunctionTypeRefImpl(
    override val source: FirSourceElement?,
    override val annotations: MutableList<FirAnnotationCall>,
    override val isMarkedNullable: Boolean,
    override var receiverTypeRef: FirTypeRef?,
    override val valueParameters: MutableList<FirValueParameter>,
    override var returnTypeRef: FirTypeRef,
    override val isSuspend: Boolean,
) : FirFunctionTypeRef() {
    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        receiverTypeRef?.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }
        returnTypeRef.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirFunctionTypeRefImpl {
        transformAnnotations(transformer, data)
        receiverTypeRef = receiverTypeRef?.transformSingle(transformer, data)
        valueParameters.transformInplace(transformer, data)
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirFunctionTypeRefImpl {
        annotations.transformInplace(transformer, data)
        return this
    }
}
