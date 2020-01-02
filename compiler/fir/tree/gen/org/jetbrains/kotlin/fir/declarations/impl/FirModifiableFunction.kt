/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.impl.FirEmptyControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirModifiableFunction<F : FirFunction<F>>  : FirFunction<F>, FirAbstractAnnotatedElement {
    override val source: FirSourceElement?
    override val session: FirSession
    override var resolvePhase: FirResolvePhase
    override val annotations: MutableList<FirAnnotationCall>
    override var returnTypeRef: FirTypeRef
    override var receiverTypeRef: FirTypeRef?
    override var controlFlowGraphReference: FirControlFlowGraphReference
    override val typeParameters: MutableList<FirTypeParameter>
    override val symbol: FirFunctionSymbol<F>
    override val valueParameters: MutableList<FirValueParameter>
    override var body: FirBlock?
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirModifiableFunction<F>

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirModifiableFunction<F>

    override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirModifiableFunction<F>

    override fun <D> transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirModifiableFunction<F>

    override fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirModifiableFunction<F>

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef)
}
