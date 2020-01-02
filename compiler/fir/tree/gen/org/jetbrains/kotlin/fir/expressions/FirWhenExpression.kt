/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirWhenExpression : FirPureAbstractElement(), FirExpression, FirResolvable {
    abstract override val source: FirSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotationCall>
    abstract override val calleeReference: FirReference
    abstract val subject: FirExpression?
    abstract val subjectVariable: FirVariable<*>?
    abstract val branches: List<FirWhenBranch>
    abstract val isExhaustive: Boolean

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitWhenExpression(this, data)

    abstract fun replaceIsExhaustive(newIsExhaustive: Boolean)

    abstract override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirWhenExpression

    abstract fun <D> transformSubject(transformer: FirTransformer<D>, data: D): FirWhenExpression

    abstract fun <D> transformBranches(transformer: FirTransformer<D>, data: D): FirWhenExpression

    abstract fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirWhenExpression
}
