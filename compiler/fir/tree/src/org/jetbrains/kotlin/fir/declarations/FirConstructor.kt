/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.expressions.FirAnnotationContainer
import org.jetbrains.kotlin.fir.expressions.FirConstructorCall
import org.jetbrains.kotlin.fir.visitors.FirVisitor

interface FirConstructor : FirFunction, FirAnnotationContainer {
    val delegatedConstructor: FirConstructorCall?

    val visibility: Visibility

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitConstructor(this, data)

    override fun <D> acceptChildren(visitor: FirVisitor<Unit, D>, data: D) {
        acceptAnnotations(visitor, data)
        delegatedConstructor?.accept(visitor, data)
        super.acceptChildren(visitor, data)
    }
}