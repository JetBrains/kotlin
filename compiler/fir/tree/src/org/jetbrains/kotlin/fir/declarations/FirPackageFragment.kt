/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.visitors.FirVisitor

interface FirPackageFragment : FirElement, FirDeclarationContainer {
    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitPackageFragment(this, data)

    override fun <D> acceptChildren(visitor: FirVisitor<Unit, D>, data: D) {
        for (declaration in declarations) {
            declaration.accept(visitor, data)
        }
        super.acceptChildren(visitor, data)
    }
}