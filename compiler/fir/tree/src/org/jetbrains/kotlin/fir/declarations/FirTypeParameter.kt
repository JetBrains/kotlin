/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.expressions.FirAnnotationContainer
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.types.Variance

interface FirTypeParameter : FirNamedDeclaration, FirAnnotationContainer {
    val variance: Variance

    val bounds: List<FirType>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitTypeParameter(this, data)

    override fun <D> acceptChildren(visitor: FirVisitor<Unit, D>, data: D) {
        acceptAnnotations(visitor, data)
        super.acceptChildren(visitor, data)
        for (bound in bounds) {
            bound.accept(visitor, data)
        }
    }
}