/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

interface FirCallableMemberDeclaration<F : FirCallableMemberDeclaration<F>> :
    @VisitedSupertype FirDeclaration, FirMemberDeclaration, FirCallableDeclaration<F> {

    val isOverride: Boolean get() = status.isOverride

    val isStatic: Boolean get() = status.isStatic

    val containerSource: DeserializedContainerSource? get() = null

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitCallableMemberDeclaration(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        receiverTypeRef?.accept(visitor, data)
        super<FirMemberDeclaration>.acceptChildren(visitor, data)
        returnTypeRef.accept(visitor, data)
    }


}
