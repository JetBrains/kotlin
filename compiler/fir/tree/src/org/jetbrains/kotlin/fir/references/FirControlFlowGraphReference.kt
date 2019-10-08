/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.BaseTransformedType
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.FirReference
import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.visitors.FirVisitor

@BaseTransformedType
abstract class FirControlFlowGraphReference : FirPureAbstractElement(), @VisitedSupertype FirReference {
    override val psi: PsiElement? get() = null

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R {
        return visitor.visitControlFlowGraphReference(this, data)
    }
}

