/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirTypeParametersOwner
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirModifiableTypeParametersOwner : FirTypeParametersOwner {
    override val source: FirSourceElement?
    override val typeParameters: MutableList<FirTypeParameter>
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirModifiableTypeParametersOwner
}
