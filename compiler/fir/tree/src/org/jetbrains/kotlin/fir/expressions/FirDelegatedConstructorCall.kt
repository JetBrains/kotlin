/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.expressions.impl.FirCallWithArgumentList
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirVisitor

abstract class FirDelegatedConstructorCall(
    psi: PsiElement?
) : @VisitedSupertype FirCallWithArgumentList(psi), FirQualifiedAccess {
    // Do we need 'constructedType: FirType' here?
    abstract val constructedTypeRef: FirTypeRef

    abstract val isThis: Boolean

    val isSuper: Boolean
        get() = !isThis

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitDelegatedConstructorCall(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        constructedTypeRef.accept(visitor, data)
        calleeReference.accept(visitor, data)
        super<FirCallWithArgumentList>.acceptChildren(visitor, data)
    }
}