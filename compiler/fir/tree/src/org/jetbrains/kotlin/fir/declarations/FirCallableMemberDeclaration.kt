/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.visitors.FirVisitor

interface FirCallableMemberDeclaration : @VisitedSupertype FirDeclaration, FirMemberDeclaration, FirCallableDeclaration {

    val isOverride: Boolean get() = status.isOverride

    val isStatic: Boolean get() = status.isStatic

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitCallableMemberDeclaration(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        receiverTypeRef?.accept(visitor, data)
        super<FirMemberDeclaration>.acceptChildren(visitor, data)
        returnTypeRef.accept(visitor, data)
    }


}
