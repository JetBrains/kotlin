/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitor

interface FirMemberFunction<F : FirMemberFunction<F>> :
    @VisitedSupertype FirFunction<F>, FirCallableMemberDeclaration<F> {

    override val symbol: FirFunctionSymbol<F>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R {
        return visitor.visitMemberFunction(this, data)
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        super<FirCallableMemberDeclaration>.acceptChildren(visitor, data)
        for (parameter in valueParameters) {
            parameter.accept(visitor, data)
        }
        body?.accept(visitor, data)
        controlFlowGraphReference?.accept(visitor, data)
    }
}