/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirLabeledElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.expressions.impl.FirUnknownTypeExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirVisitor

abstract class FirAnonymousFunction(
    final override val session: FirSession,
    psi: PsiElement?
) : @VisitedSupertype FirFunction<FirAnonymousFunction>, FirUnknownTypeExpression(psi), FirLabeledElement {
    abstract override val receiverTypeRef: FirTypeRef?

    abstract override val symbol: FirAnonymousFunctionSymbol

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitAnonymousFunction(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        super<FirFunction>.acceptChildren(visitor, data)
        super<FirLabeledElement>.acceptChildren(visitor, data)
        typeRef.accept(visitor, data)
        // Don't call super<FirExpression>.acceptChildren (annotations & typeRef are already processed)
    }

    abstract fun replaceReceiverTypeRef(receiverTypeRef: FirTypeRef)
}