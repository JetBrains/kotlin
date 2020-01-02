/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirCallableDeclaration<F : FirCallableDeclaration<F>> : FirTypedDeclaration, FirSymbolOwner<F> {
    override val source: FirSourceElement?
    override val session: FirSession
    override val resolvePhase: FirResolvePhase
    override val annotations: List<FirAnnotationCall>
    override val returnTypeRef: FirTypeRef
    val receiverTypeRef: FirTypeRef?
    override val symbol: FirCallableSymbol<F>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitCallableDeclaration(this, data)

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirCallableDeclaration<F>

    fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirCallableDeclaration<F>
}
