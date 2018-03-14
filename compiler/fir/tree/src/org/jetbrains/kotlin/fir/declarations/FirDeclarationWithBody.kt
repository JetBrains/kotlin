/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.expressions.FirBody
import org.jetbrains.kotlin.fir.visitors.FirVisitor

interface FirDeclarationWithBody : FirDeclaration {
    val body: FirBody?

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitDeclarationWithBody(this, data)

    override fun <D> acceptChildren(visitor: FirVisitor<Unit, D>, data: D) {
        body?.accept(visitor, data)
        super.acceptChildren(visitor, data)
    }
}