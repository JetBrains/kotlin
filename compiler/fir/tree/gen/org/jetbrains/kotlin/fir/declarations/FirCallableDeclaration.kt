/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

sealed class FirCallableDeclaration<E : FirCallableDeclaration<E>> : FirTypedDeclaration<E>() {
    abstract override val source: FirSourceElement?
    abstract override val moduleData: FirModuleData
    abstract override val resolvePhase: FirResolvePhase
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val annotations: List<FirAnnotationCall>
    abstract override val returnTypeRef: FirTypeRef
    abstract val receiverTypeRef: FirTypeRef?
    abstract override val symbol: FirCallableSymbol<E>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitCallableDeclaration(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E: FirElement, D> transform(transformer: FirTransformer<D>, data: D): E = 
        transformer.transformCallableDeclaration(this, data) as E

    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    abstract override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef)

    abstract fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirCallableDeclaration<E>

    abstract override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirCallableDeclaration<E>

    abstract fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirCallableDeclaration<E>
}
