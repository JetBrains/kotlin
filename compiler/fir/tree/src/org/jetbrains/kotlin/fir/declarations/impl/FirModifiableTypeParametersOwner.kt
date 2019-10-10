/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirTypeParametersOwner
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirModifiableTypeParametersOwner : FirTypeParametersOwner {
    override val psi: PsiElement?
    override val typeParameters: MutableList<FirTypeParameter>
    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        typeParameters.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirModifiableTypeParametersOwner {
        typeParameters.transformInplace(transformer, data)
        return this
    }
}
