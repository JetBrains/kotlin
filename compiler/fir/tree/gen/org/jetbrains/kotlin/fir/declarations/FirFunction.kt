/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.FirTargetElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirFunction<F : FirFunction<F>> : FirCallableDeclaration<F>, FirTargetElement, FirAnnotationContainer, FirTypeParameterRefsOwner, FirStatement {
    override val source: FirSourceElement?
    override val session: FirSession
    override val resolvePhase: FirResolvePhase
    override val annotations: List<FirAnnotationCall>
    override val returnTypeRef: FirTypeRef
    override val receiverTypeRef: FirTypeRef?
    override val typeParameters: List<FirTypeParameterRef>
    val controlFlowGraphReference: FirControlFlowGraphReference
    override val symbol: FirFunctionSymbol<F>
    val valueParameters: List<FirValueParameter>
    val body: FirBlock?

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitFunction(this, data)

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef)

    override fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?)

    fun replaceValueParameters(newValueParameters: List<FirValueParameter>)

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirFunction<F>

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirFunction<F>

    override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirFunction<F>

    fun <D> transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirFunction<F>

    fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirFunction<F>
}
