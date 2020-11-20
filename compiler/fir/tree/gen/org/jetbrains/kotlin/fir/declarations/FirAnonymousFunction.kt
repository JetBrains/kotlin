/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirAnonymousFunction : FirFunction<FirAnonymousFunction>, FirExpression(), FirTypeParametersOwner {
    abstract override val source: FirSourceElement?
    abstract override val session: FirSession
    abstract override val resolvePhase: FirResolvePhase
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val annotations: List<FirAnnotationCall>
    abstract override val returnTypeRef: FirTypeRef
    abstract override val receiverTypeRef: FirTypeRef?
    abstract override val controlFlowGraphReference: FirControlFlowGraphReference?
    abstract override val valueParameters: List<FirValueParameter>
    abstract override val body: FirBlock?
    abstract override val typeRef: FirTypeRef
    abstract override val symbol: FirAnonymousFunctionSymbol
    abstract val label: FirLabel?
    abstract val invocationKind: EventOccurrencesRange?
    abstract val isLambda: Boolean
    abstract override val typeParameters: List<FirTypeParameter>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitAnonymousFunction(this, data)

    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    abstract override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef)

    abstract override fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?)

    abstract override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?)

    abstract override fun replaceValueParameters(newValueParameters: List<FirValueParameter>)

    abstract override fun replaceBody(newBody: FirBlock?)

    abstract override fun replaceTypeRef(newTypeRef: FirTypeRef)

    abstract fun replaceInvocationKind(newInvocationKind: EventOccurrencesRange?)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirAnonymousFunction

    abstract override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirAnonymousFunction

    abstract override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirAnonymousFunction

    abstract override fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirAnonymousFunction

    abstract override fun <D> transformBody(transformer: FirTransformer<D>, data: D): FirAnonymousFunction

    abstract override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirAnonymousFunction
}
