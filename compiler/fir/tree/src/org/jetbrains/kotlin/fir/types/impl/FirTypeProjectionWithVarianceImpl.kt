/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirAbstractElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.types.Variance

class FirTypeProjectionWithVarianceImpl(
    psi: PsiElement?,
    override val variance: Variance,
    override var typeRef: FirTypeRef
) : FirTypeProjectionWithVariance, FirAbstractElement(psi) {
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        typeRef = typeRef.transformSingle(transformer, data)

        return this
    }
}