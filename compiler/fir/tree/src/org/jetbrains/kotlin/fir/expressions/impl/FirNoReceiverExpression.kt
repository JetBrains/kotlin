/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.FirVisitor

object FirNoReceiverExpression : FirExpression(null) {
    override val typeRef: FirTypeRef = FirImplicitTypeRefImpl(null)

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {}

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}
}