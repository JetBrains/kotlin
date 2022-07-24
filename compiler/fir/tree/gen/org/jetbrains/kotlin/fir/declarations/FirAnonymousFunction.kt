/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

abstract class FirAnonymousFunction : FirFunction(), FirTypeParameterRefsOwner {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract override val moduleData: FirModuleData
    abstract override val resolvePhase: FirResolvePhase
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val status: FirDeclarationStatus
    abstract override val returnTypeRef: FirTypeRef
    abstract override val receiverTypeRef: FirTypeRef?
    abstract override val deprecation: DeprecationsPerUseSite?
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
    abstract override val typeParameters: List<FirTypeParameterRef>
    abstract val typeRef: FirTypeRef


    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    abstract override fun replaceStatus(newStatus: FirDeclarationStatus)

    abstract override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef)

    abstract override fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?)

    abstract override fun replaceDeprecation(newDeprecation: DeprecationsPerUseSite?)

    abstract override fun replaceContextReceivers(newContextReceivers: List<FirContextReceiver>)

    abstract override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?)

    abstract override fun replaceValueParameters(newValueParameters: List<FirValueParameter>)

    abstract override fun replaceBody(newBody: FirBlock?)

    abstract fun replaceLabel(newLabel: FirLabel?)

    abstract fun replaceInvocationKind(newInvocationKind: EventOccurrencesRange?)

    abstract fun replaceInlineStatus(newInlineStatus: InlineStatus)

    abstract override fun replaceTypeParameters(newTypeParameters: List<FirTypeParameterRef>)

    abstract fun replaceTypeRef(newTypeRef: FirTypeRef)
}

inline fun <D> FirAnonymousFunction.transformAnnotations(transformer: FirTransformer<D>, data: D): FirAnonymousFunction 
     = apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirAnonymousFunction.transformStatus(transformer: FirTransformer<D>, data: D): FirAnonymousFunction 
     = apply { replaceStatus(status.transform(transformer, data)) }

inline fun <D> FirAnonymousFunction.transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirAnonymousFunction 
     = apply { replaceReturnTypeRef(returnTypeRef.transform(transformer, data)) }

inline fun <D> FirAnonymousFunction.transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirAnonymousFunction 
     = apply { replaceReceiverTypeRef(receiverTypeRef?.transform(transformer, data)) }

inline fun <D> FirAnonymousFunction.transformContextReceivers(transformer: FirTransformer<D>, data: D): FirAnonymousFunction 
     = apply { replaceContextReceivers(contextReceivers.transform(transformer, data)) }

inline fun <D> FirAnonymousFunction.transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirAnonymousFunction 
     = apply { replaceControlFlowGraphReference(controlFlowGraphReference?.transform(transformer, data)) }

inline fun <D> FirAnonymousFunction.transformValueParameters(transformer: FirTransformer<D>, data: D): FirAnonymousFunction 
     = apply { replaceValueParameters(valueParameters.transform(transformer, data)) }

inline fun <D> FirAnonymousFunction.transformBody(transformer: FirTransformer<D>, data: D): FirAnonymousFunction 
     = apply { replaceBody(body?.transform(transformer, data)) }

inline fun <D> FirAnonymousFunction.transformLabel(transformer: FirTransformer<D>, data: D): FirAnonymousFunction 
     = apply { replaceLabel(label?.transform(transformer, data)) }

inline fun <D> FirAnonymousFunction.transformTypeParameters(transformer: FirTransformer<D>, data: D): FirAnonymousFunction 
     = apply { replaceTypeParameters(typeParameters.transform(transformer, data)) }

inline fun <D> FirAnonymousFunction.transformTypeRef(transformer: FirTransformer<D>, data: D): FirAnonymousFunction 
     = apply { replaceTypeRef(typeRef.transform(transformer, data)) }
