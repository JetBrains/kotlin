/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirElement
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

sealed interface FirCallableDeclaration<F : FirCallableDeclaration<F>> : FirTypedDeclaration, FirSymbolOwner<F> {
    override val source: FirSourceElement?
    override val declarationSiteSession: FirSession
    override val resolvePhase: FirResolvePhase
    override val origin: FirDeclarationOrigin
    override val attributes: FirDeclarationAttributes
    override val annotations: List<FirAnnotationCall>
    override val returnTypeRef: FirTypeRef
    val receiverTypeRef: FirTypeRef?
    override val symbol: FirCallableSymbol<F>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitCallableDeclaration(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E: FirElement, D> transform(transformer: FirTransformer<D>, data: D): E = 
        transformer.transformCallableDeclaration(this, data) as E

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef)

    fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?)

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirCallableDeclaration<F>

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirCallableDeclaration<F>

    fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirCallableDeclaration<F>
}
