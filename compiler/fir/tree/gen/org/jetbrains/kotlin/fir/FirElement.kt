/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.jvm.specialization.annotations.Monomorphic

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirElement {
    val source: KtSourceElement?

    fun <R, D, @Monomorphic VT : FirVisitor<R, D>> accept(visitor: VT, data: D): R = visitor.visitElement(this, data)

    @Suppress("UNCHECKED_CAST")
    fun <E: FirElement, D, @Monomorphic TT: FirTransformer<D>> transform(transformer: TT, data: D): E = 
        transformer.transformElement(this, data) as E

    fun <@Monomorphic VT: FirVisitorVoid> accept(visitor: VT) = accept(visitor, null)

    fun <R, D, @Monomorphic VT : FirVisitor<R, D>> acceptChildren(visitor: VT, data: D)

    fun <@Monomorphic VT: FirVisitorVoid> acceptChildren(visitor: VT) = acceptChildren(visitor, null)

    fun <D, @Monomorphic TT: FirTransformer<D>> transformChildren(transformer: TT, data: D): FirElement
}
