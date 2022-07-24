/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.references

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirSuperReference : FirReference() {
    abstract override val source: KtSourceElement?
    abstract val labelName: String?
    abstract val superTypeRef: FirTypeRef


    abstract fun replaceSuperTypeRef(newSuperTypeRef: FirTypeRef)
}

inline fun <D> FirSuperReference.transformSuperTypeRef(transformer: FirTransformer<D>, data: D): FirSuperReference 
     = apply { replaceSuperTypeRef(superTypeRef.transform(transformer, data)) }
