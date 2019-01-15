/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.BaseTransformedType
import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.visitors.FirVisitor

@BaseTransformedType
interface FirEnumEntry : @VisitedSupertype FirRegularClass, FirCall {
    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitEnumEntry(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        for (argument in arguments) {
            argument.accept(visitor, data)
        }
        super<FirRegularClass>.acceptChildren(visitor, data)
    }
}