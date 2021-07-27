/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.references.impl

import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirExplicitSuperReference @FirImplementationDetail constructor(
    override val source: FirSourceElement?,
    override val labelName: String?,
    override var superTypeRef: FirTypeRef,
) : FirSuperReference() {
    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        superTypeRef.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirExplicitSuperReference {
        superTypeRef = superTypeRef.transform(transformer, data)
        return this
    }

    override fun replaceSuperTypeRef(newSuperTypeRef: FirTypeRef) {
        superTypeRef = newSuperTypeRef
    }
}
