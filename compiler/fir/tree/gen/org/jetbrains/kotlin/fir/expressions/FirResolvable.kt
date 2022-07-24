/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

sealed interface FirResolvable : FirElement {
    override val source: KtSourceElement?
    val calleeReference: FirReference


    fun replaceCalleeReference(newCalleeReference: FirReference)
}

inline fun <D> FirResolvable.transformCalleeReference(transformer: FirTransformer<D>, data: D): FirResolvable 
     = apply { replaceCalleeReference(calleeReference.transform(transformer, data)) }
