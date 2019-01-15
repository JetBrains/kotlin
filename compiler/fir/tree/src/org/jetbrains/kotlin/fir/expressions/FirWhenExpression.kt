/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.visitors.FirVisitor

interface FirWhenExpression : FirExpression {
    val subject: FirExpression?

    // when (val subjectVariable = subject()) { ... }
    val subjectVariable: FirVariable?

    val branches: List<FirWhenBranch>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitWhenExpression(this, data)


    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        subjectVariable?.accept(visitor, data) ?: subject?.accept(visitor, data)
        for (branch in branches) {
            branch.accept(visitor, data)
        }
        super.acceptChildren(visitor, data)
    }
}