/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirSafeCallCheckedSubjectReference
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirSafeCallExpressionImpl(
    override val source: FirSourceElement?,
    override var typeRef: FirTypeRef,
    override val annotations: MutableList<FirAnnotationCall>,
    override var receiver: FirExpression,
    override val checkedSubject: FirSafeCallCheckedSubjectReference,
    override var regularQualifiedAccess: FirQualifiedAccess,
) : FirSafeCallExpression() {
    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        typeRef.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        receiver.accept(visitor, data)
        regularQualifiedAccess.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirSafeCallExpressionImpl {
        typeRef = typeRef.transformSingle(transformer, data)
        transformAnnotations(transformer, data)
        transformReceiver(transformer, data)
        transformRegularQualifiedAccess(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirSafeCallExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformReceiver(transformer: FirTransformer<D>, data: D): FirSafeCallExpressionImpl {
        receiver = receiver.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformRegularQualifiedAccess(transformer: FirTransformer<D>, data: D): FirSafeCallExpressionImpl {
        regularQualifiedAccess = regularQualifiedAccess.transformSingle(transformer, data)
        return this
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }

    override fun replaceRegularQualifiedAccess(newRegularQualifiedAccess: FirQualifiedAccess) {
        regularQualifiedAccess = newRegularQualifiedAccess
    }
}
