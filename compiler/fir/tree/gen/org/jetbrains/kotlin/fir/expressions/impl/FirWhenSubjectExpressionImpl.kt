/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.FirWhenSubject
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirWhenSubjectExpression
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirWhenSubjectExpressionImpl(
    override val source: FirSourceElement?,
    override val whenSubject: FirWhenSubject
) : FirWhenSubjectExpression(), FirAbstractAnnotatedElement {
    override val typeRef: FirTypeRef get() = whenSubject.whenExpression.subject!!.typeRef
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirWhenSubjectExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {}
}
