/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirNamedReference
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.expressions.impl.FirUnknownTypeCallWithArgumentList
import org.jetbrains.kotlin.fir.types.FirTypeProjectionContainer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

abstract class FirFunctionCall(
    session: FirSession,
    psi: PsiElement?
) : @VisitedSupertype FirUnknownTypeCallWithArgumentList(session, psi), FirQualifiedAccess, FirTypeProjectionContainer {
    abstract override val calleeReference: FirNamedReference

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitFunctionCall(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        calleeReference.accept(visitor, data)
        explicitReceiver?.accept(visitor, data)
        for (typeArgument in typeArguments) {
            typeArgument.accept(visitor, data)
        }
        super<FirUnknownTypeCallWithArgumentList>.acceptChildren(visitor, data)
    }
}