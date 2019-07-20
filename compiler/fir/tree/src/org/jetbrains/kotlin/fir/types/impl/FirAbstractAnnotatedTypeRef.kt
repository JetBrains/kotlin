/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirAbstractElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.transformInplace
import org.jetbrains.kotlin.fir.types.FirTypeRefWithNullability
import org.jetbrains.kotlin.fir.visitors.FirTransformer

abstract class FirAbstractAnnotatedTypeRef(
    psi: PsiElement?,
    final override val isMarkedNullable: Boolean
) : FirTypeRefWithNullability, FirAbstractElement(psi) {
    override val annotations = mutableListOf<FirAnnotationCall>()

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        annotations.transformInplace(transformer, data)

        return this
    }
}