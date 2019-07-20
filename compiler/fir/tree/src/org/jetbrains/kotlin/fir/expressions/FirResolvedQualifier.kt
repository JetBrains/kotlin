/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.expressions.impl.FirUnknownTypeExpression
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

abstract class FirResolvedQualifier(psi: PsiElement?) : FirUnknownTypeExpression(psi) {
    abstract val packageFqName: FqName

    abstract val relativeClassFqName: FqName?

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitResolvedQualifier(this, data)

    val classId
        get() = relativeClassFqName?.let {
            ClassId(packageFqName, it, false)
        }
}