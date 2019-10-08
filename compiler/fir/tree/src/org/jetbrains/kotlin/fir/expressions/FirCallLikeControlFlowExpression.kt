/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.expressions.impl.FirUnknownTypeExpression
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

abstract class FirCallLikeControlFlowExpression(psi: PsiElement?) : @VisitedSupertype FirUnknownTypeExpression(psi), FirResolvable {
    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        super<FirUnknownTypeExpression>.acceptChildren(visitor, data)
    }

    override fun accept(visitor: FirVisitorVoid) {
        super<FirUnknownTypeExpression>.accept(visitor)
    }

    override fun acceptChildren(visitor: FirVisitorVoid) {
        super<FirUnknownTypeExpression>.acceptChildren(visitor)
    }

    override fun <E : FirElement, D> transform(visitor: FirTransformer<D>, data: D): CompositeTransformResult<E> {
        return super<FirUnknownTypeExpression>.transform(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        return super<FirUnknownTypeExpression>.transformChildren(transformer, data)
    }
}