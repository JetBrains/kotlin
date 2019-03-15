/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.visitors.FirVisitor

// Target element which may have label
interface FirLabeledElement : FirTargetElement {
    val label: FirLabel? get() = null

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitLabeledElement(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        label?.accept(visitor, data)
    }

}