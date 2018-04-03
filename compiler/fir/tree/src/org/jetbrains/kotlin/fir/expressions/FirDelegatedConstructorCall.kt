/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.types.FirTypeProjectionContainer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

interface FirDelegatedConstructorCall : FirCall {
    // Do we need 'constructedType: FirType' here?
    val constructedType: FirType

    val isThis: Boolean

    val isSuper: Boolean
        get() = !isThis

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitDelegatedConstructorCall(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        constructedType.accept(visitor, data)
        super.acceptChildren(visitor, data)
    }
}