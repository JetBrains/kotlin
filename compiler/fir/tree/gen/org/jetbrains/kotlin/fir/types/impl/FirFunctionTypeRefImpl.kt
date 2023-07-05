/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirFunctionTypeParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.FirFunctionTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.fir.MutableOrEmptyList
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirFunctionTypeRefImpl(
    override val source: KtSourceElement?,
    override var annotations: MutableOrEmptyList<FirAnnotation>,
    override val isMarkedNullable: Boolean,
    override var receiverTypeRef: FirTypeRef?,
    override val parameters: MutableList<FirFunctionTypeParameter>,
    override var returnTypeRef: FirTypeRef,
    override val isSuspend: Boolean,
    override val contextReceiverTypeRefs: MutableList<FirTypeRef>,
) : FirFunctionTypeRef() {
    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        receiverTypeRef?.accept(visitor, data)
        parameters.forEach { it.accept(visitor, data) }
        returnTypeRef.accept(visitor, data)
        contextReceiverTypeRefs.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirFunctionTypeRefImpl {
        transformAnnotations(transformer, data)
        receiverTypeRef = receiverTypeRef?.transform(transformer, data)
        parameters.transformInplace(transformer, data)
        returnTypeRef = returnTypeRef.transform(transformer, data)
        contextReceiverTypeRefs.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirFunctionTypeRefImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }
}
