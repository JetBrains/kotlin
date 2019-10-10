/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirOperatorCall : FirExpression, FirCall {
    override val psi: PsiElement?
    override val typeRef: FirTypeRef
    override val annotations: List<FirAnnotationCall>
    override val arguments: List<FirExpression>
    val operation: FirOperation

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitOperatorCall(this, data)

    override fun <D> transformArguments(transformer: FirTransformer<D>, data: D): FirOperatorCall
}
