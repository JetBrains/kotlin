/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirSafeCallCheckedSubjectReference
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirSafeCallExpression : FirExpression() {
    abstract override val source: FirSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotationCall>
    abstract val receiver: FirExpression
    abstract val checkedSubject: FirSafeCallCheckedSubjectReference
    abstract val regularQualifiedAccess: FirQualifiedAccess

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitSafeCallExpression(this, data)

    abstract override fun replaceTypeRef(newTypeRef: FirTypeRef)

    abstract fun replaceRegularQualifiedAccess(newRegularQualifiedAccess: FirQualifiedAccess)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirSafeCallExpression

    abstract fun <D> transformReceiver(transformer: FirTransformer<D>, data: D): FirSafeCallExpression

    abstract fun <D> transformRegularQualifiedAccess(transformer: FirTransformer<D>, data: D): FirSafeCallExpression
}
