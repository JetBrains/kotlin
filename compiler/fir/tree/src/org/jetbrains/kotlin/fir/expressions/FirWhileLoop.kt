/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirWhileLoop : FirLoop {
    override val psi: PsiElement?
    override val annotations: List<FirAnnotationCall>
    override val label: FirLabel?
    override val condition: FirExpression
    override val block: FirBlock

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitWhileLoop(this, data)

    override fun <D> transformCondition(transformer: FirTransformer<D>, data: D): FirWhileLoop

    override fun <D> transformBlock(transformer: FirTransformer<D>, data: D): FirWhileLoop

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirWhileLoop
}
