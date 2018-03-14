/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.visitors.FirVisitor

// Good name needed (something with receiver, type parameters, return type, and name)
interface FirCallableMember : @VisitedSupertype FirDeclaration, FirMemberDeclaration, FirTypedDeclaration {
    val isOverride: Boolean

    val receiverType: FirType?

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitCallableMember(this, data)

    override fun <D> acceptChildren(visitor: FirVisitor<Unit, D>, data: D) {
        receiverType?.accept(visitor, data)
        super<FirMemberDeclaration>.acceptChildren(visitor, data)
        returnType.accept(visitor, data)
    }
}