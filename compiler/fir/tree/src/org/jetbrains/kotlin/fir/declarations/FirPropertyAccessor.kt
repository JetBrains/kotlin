/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.visitors.FirVisitor

interface FirPropertyAccessor : @VisitedSupertype FirFunction, FirTypedDeclaration {
    val isGetter: Boolean

    val isSetter: Boolean get() = !isGetter

    val status: FirDeclarationStatus

    val visibility: Visibility get() = status.visibility

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitPropertyAccessor(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        super<FirTypedDeclaration>.acceptChildren(visitor, data)
        for (parameter in valueParameters) {
            parameter.accept(visitor, data)
        }
        body?.accept(visitor, data)
        status.accept(visitor, data)
    }
}