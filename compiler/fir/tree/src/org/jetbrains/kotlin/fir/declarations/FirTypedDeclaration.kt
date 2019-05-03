/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.expressions.FirAnnotationContainer
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

interface FirTypedDeclaration : FirDeclaration, FirAnnotationContainer {
    val returnTypeRef: FirTypeRef

    fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D)

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitTypedDeclaration(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        acceptAnnotations(visitor, data)
        super.acceptChildren(visitor, data)
        returnTypeRef.accept(visitor, data)
    }
}