/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

sealed interface FirStatusOwner : FirTypeParameterRefsOwner {
    override val source: FirSourceElement?
    override val typeParameters: List<FirTypeParameterRef>
    val status: FirDeclarationStatus

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitStatusOwner(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E: FirElement, D> transform(transformer: FirTransformer<D>, data: D): E = 
        transformer.transformStatusOwner(this, data) as E

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirStatusOwner

    fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirStatusOwner
}
