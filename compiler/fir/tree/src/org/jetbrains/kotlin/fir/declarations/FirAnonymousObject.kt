/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.expressions.impl.FirUnknownTypeExpression
import org.jetbrains.kotlin.fir.visitors.FirVisitor

abstract class FirAnonymousObject(psi: PsiElement?) : @VisitedSupertype FirClass, FirUnknownTypeExpression(psi) {
    override val classKind: ClassKind
        get() = ClassKind.OBJECT

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitAnonymousObject(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        super<FirClass>.acceptChildren(visitor, data)
        super<FirUnknownTypeExpression>.acceptChildren(visitor, data)
    }
}