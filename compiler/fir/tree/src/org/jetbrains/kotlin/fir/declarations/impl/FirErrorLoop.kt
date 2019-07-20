/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirAbstractElement
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyExpressionBlock
import org.jetbrains.kotlin.fir.expressions.impl.FirErrorExpressionImpl
import org.jetbrains.kotlin.fir.visitors.FirVisitor

class FirErrorLoop(
    psi: PsiElement?,
    override val reason: String
) : FirAbstractElement(psi), FirErrorStatement, FirLoop {
    override val annotations: List<FirAnnotationCall> = listOf()

    override val condition: FirExpression = FirErrorExpressionImpl(psi, reason)

    override val block: FirBlock = FirEmptyExpressionBlock()

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R {
        return super<FirErrorStatement>.accept(visitor, data)
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        super<FirLoop>.acceptChildren(visitor, data)
    }
}