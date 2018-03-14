/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.expressions.FirAnnotationContainer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

// May be should inherit FirTypeParameterContainer
interface FirFunction : FirDeclarationWithBody, FirAnnotationContainer {
    val valueParameters: List<FirValueParameter>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitFunction(this, data)

    override fun <D> acceptChildren(visitor: FirVisitor<Unit, D>, data: D) {
        acceptAnnotations(visitor, data)
        for (parameter in valueParameters) {
            parameter.accept(visitor, data)
        }
        super.acceptChildren(visitor, data)
    }
}