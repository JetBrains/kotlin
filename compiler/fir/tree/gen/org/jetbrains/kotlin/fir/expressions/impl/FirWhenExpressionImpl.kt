/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.impl.FirStubReference
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirWhenExpressionImpl(
    override val source: FirSourceElement?,
    override var subject: FirExpression?,
    override var subjectVariable: FirVariable<*>?
) : FirWhenExpression(), FirAbstractAnnotatedElement {
    override var typeRef: FirTypeRef = FirImplicitTypeRefImpl(null)
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override var calleeReference: FirReference = FirStubReference()
    override val branches: MutableList<FirWhenBranch> = mutableListOf()
    override var isExhaustive: Boolean = false

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        typeRef.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        calleeReference.accept(visitor, data)
        if (subjectVariable != null) {
            subjectVariable.accept(visitor, data)
        } else {
            subject?.accept(visitor, data)
        }
        branches.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirWhenExpressionImpl {
        transformCalleeReference(transformer, data)
        transformSubject(transformer, data)
        transformBranches(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirWhenExpressionImpl {
        calleeReference = calleeReference.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformSubject(transformer: FirTransformer<D>, data: D): FirWhenExpressionImpl {
        if (subjectVariable != null) {
            subjectVariable = subjectVariable?.transformSingle(transformer, data)
        } else {
            subject = subject?.transformSingle(transformer, data)
        }
        return this
    }

    override fun <D> transformBranches(transformer: FirTransformer<D>, data: D): FirWhenExpressionImpl {
        branches.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirWhenExpressionImpl {
        typeRef = typeRef.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }

    override fun replaceIsExhaustive(newIsExhaustive: Boolean) {
        isExhaustive = newIsExhaustive
    }
}
