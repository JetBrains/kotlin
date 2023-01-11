/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.jvm.specialization.annotations.Monomorphic

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirStatement : FirAnnotationContainer {
    override val source: KtSourceElement?
    override val annotations: List<FirAnnotation>

    override fun <R, D, @Monomorphic VT : FirVisitor<R, D>> accept(visitor: VT, data: D): R = visitor.visitStatement(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E: FirElement, D, @Monomorphic TT: FirTransformer<D>> transform(transformer: TT, data: D): E = 
        transformer.transformStatement(this, data) as E

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirStatement
}
