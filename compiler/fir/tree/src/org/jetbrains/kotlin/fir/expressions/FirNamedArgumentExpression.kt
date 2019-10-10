/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirNamedArgumentExpression : FirWrappedArgumentExpression {
    override val psi: PsiElement?
    override val typeRef: FirTypeRef
    override val annotations: List<FirAnnotationCall>
    override val expression: FirExpression
    override val isSpread: Boolean
    val name: Name

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitNamedArgumentExpression(this, data)
}
