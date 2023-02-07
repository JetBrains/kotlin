/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirAnonymousFunction : FirFunction(), FirTypeParametersOwner {
    abstract override val source: KtSourceElement?
    abstract override val resolvePhase: FirResolvePhase
    abstract override val annotations: List<FirAnnotation>
    abstract override val moduleData: FirModuleData
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val status: FirDeclarationStatus
    abstract override val returnTypeRef: FirTypeRef
    abstract override val receiverParameter: FirReceiverParameter?
    abstract override val deprecationsProvider: DeprecationsProvider
    abstract override val containerSource: DeserializedContainerSource?
    abstract override val dispatchReceiverType: ConeSimpleKotlinType?
    abstract override val contextReceivers: List<FirContextReceiver>
    abstract override val controlFlowGraphReference: FirControlFlowGraphReference?
    abstract override val valueParameters: List<FirValueParameter>
    abstract override val body: FirBlock?
    abstract override val symbol: FirAnonymousFunctionSymbol
    abstract val label: FirLabel?
    abstract val invocationKind: EventOccurrencesRange?
    abstract val inlineStatus: InlineStatus
    abstract val isLambda: Boolean
    abstract val hasExplicitParameterList: Boolean
    abstract override val typeParameters: List<FirTypeParameter>
    abstract val typeRef: FirTypeRef

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitAnonymousFunction(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformAnonymousFunction(this, data) as E

    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceStatus(newStatus: FirDeclarationStatus)

    abstract override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef)

    abstract override fun replaceReceiverParameter(newReceiverParameter: FirReceiverParameter?)

    abstract override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider)

    abstract override fun replaceContextReceivers(newContextReceivers: List<FirContextReceiver>)

    abstract override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?)

    abstract override fun replaceValueParameters(newValueParameters: List<FirValueParameter>)

    abstract override fun replaceBody(newBody: FirBlock?)

    abstract fun replaceInvocationKind(newInvocationKind: EventOccurrencesRange?)

    abstract fun replaceInlineStatus(newInlineStatus: InlineStatus)

    abstract fun replaceTypeRef(newTypeRef: FirTypeRef)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirAnonymousFunction

    abstract override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirAnonymousFunction

    abstract override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirAnonymousFunction

    abstract override fun <D> transformReceiverParameter(transformer: FirTransformer<D>, data: D): FirAnonymousFunction

    abstract override fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirAnonymousFunction

    abstract override fun <D> transformBody(transformer: FirTransformer<D>, data: D): FirAnonymousFunction

    abstract override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirAnonymousFunction
}
