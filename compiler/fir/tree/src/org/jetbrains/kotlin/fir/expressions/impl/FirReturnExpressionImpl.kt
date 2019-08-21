/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirTarget
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitNothingTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirReturnExpressionImpl(
    psi: PsiElement?,
    override var result: FirExpression
) : FirReturnExpression(psi) {
    override lateinit var target: FirTarget<FirFunction<*>>

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        result = result.transformSingle(transformer, data)
        typeRef = typeRef.transformSingle(transformer, data)
        return super.transformChildren(transformer, data)
    }

    override var typeRef: FirTypeRef = FirImplicitNothingTypeRef(psi)

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {}
}