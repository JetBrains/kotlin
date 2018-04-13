/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.BaseTransformedType
import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.visitors.FirVisitor

@BaseTransformedType
interface FirConstructor : @VisitedSupertype FirFunction, FirCallableMember {
    val delegatedConstructor: FirDelegatedConstructorCall?

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitConstructor(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        super<FirCallableMember>.acceptChildren(visitor, data)
        delegatedConstructor?.accept(visitor, data)
        for (parameter in valueParameters) {
            parameter.accept(visitor, data)
        }
        body?.accept(visitor, data)
    }
}