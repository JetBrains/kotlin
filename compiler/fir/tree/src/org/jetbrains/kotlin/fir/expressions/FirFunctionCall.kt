/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirNamedReference
import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.types.FirTypeProjectionContainer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

interface FirFunctionCall : @VisitedSupertype FirCall, FirQualifiedAccess, FirTypeProjectionContainer {
    override val calleeReference: FirNamedReference

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitFunctionCall(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        for (argument in arguments) {
            argument.accept(visitor, data)
        }
        for (typeArgument in typeArguments) {
            typeArgument.accept(visitor, data)
        }
        super<FirQualifiedAccess>.acceptChildren(visitor, data)
    }
}