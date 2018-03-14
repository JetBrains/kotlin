/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.types.FirDelegatedType
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.visitors.FirVisitor

class FirDelegatedTypeImpl(
    override val type: FirType,
    override val delegate: FirExpression?
) : FirType by type, FirDelegatedType {
    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R {
        return super<FirDelegatedType>.accept(visitor, data)
    }

    override fun <D> acceptChildren(visitor: FirVisitor<Unit, D>, data: D) {
        type.acceptChildren(visitor, data)
        delegate?.accept(visitor, data)
    }
}