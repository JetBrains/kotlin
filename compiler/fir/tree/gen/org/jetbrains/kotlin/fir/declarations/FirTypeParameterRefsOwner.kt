/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

sealed interface FirTypeParameterRefsOwner : FirElement {
    override val source: KtSourceElement?
    val typeParameters: List<FirTypeParameterRef>


    fun replaceTypeParameters(newTypeParameters: List<FirTypeParameterRef>)
}

inline fun <D> FirTypeParameterRefsOwner.transformTypeParameters(transformer: FirTransformer<D>, data: D): FirTypeParameterRefsOwner 
     = apply { replaceTypeParameters(typeParameters.transform(transformer, data)) }
