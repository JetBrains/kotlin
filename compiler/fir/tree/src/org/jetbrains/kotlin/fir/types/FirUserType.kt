/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name

interface FirUserType : FirTypeWithNullability, FirTypeProjectionContainer {
    val name: Name

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitUserType(this, data)

    override fun <D> acceptChildren(visitor: FirVisitor<Unit, D>, data: D) {
        super.acceptChildren(visitor, data)
        for (argument in arguments) {
            argument.accept(visitor, data)
        }
    }
}