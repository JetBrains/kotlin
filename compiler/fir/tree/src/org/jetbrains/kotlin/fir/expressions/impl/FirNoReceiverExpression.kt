/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.jvm.specialization.annotations.Monomorphic

object FirNoReceiverExpression : FirExpression() {
    override val source: KtSourceElement? = null
    override val typeRef: FirTypeRef = FirImplicitTypeRefImpl(null)
    override val annotations: List<FirAnnotation> get() = emptyList()

    override fun <R, D, @Monomorphic VT : FirVisitor<R, D>> acceptChildren(visitor: VT, data: D) {}

    override fun <D, @Monomorphic TT: FirTransformer<D>> transformChildren(transformer: TT, data: D): FirNoReceiverExpression {
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirExpression {
        return this
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {}
}
