/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirWhenExpression : FirExpression, FirResolvable {
    override val psi: PsiElement?
    override val typeRef: FirTypeRef
    override val annotations: List<FirAnnotationCall>
    override val calleeReference: FirReference
    val subject: FirExpression?
    val subjectVariable: FirVariable<*>?
    val branches: List<FirWhenBranch>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitWhenExpression(this, data)

    override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirWhenExpression

    fun <D> transformSubject(transformer: FirTransformer<D>, data: D): FirWhenExpression

    fun <D> transformBranches(transformer: FirTransformer<D>, data: D): FirWhenExpression

    fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirWhenExpression
}
