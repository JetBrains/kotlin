/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirConstructor : FirFunction(), FirTypeParameterRefsOwner {
    abstract override val source: KtSourceElement?
    abstract override val moduleData: FirModuleData
    abstract override val resolvePhase: FirResolvePhase
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val typeParameters: List<FirTypeParameterRef>
    abstract override val status: FirDeclarationStatus
    abstract override val returnTypeRef: FirTypeRef
    abstract override val receiverTypeRef: FirTypeRef?
    abstract override val deprecation: DeprecationsPerUseSite?
    abstract override val containerSource: DeserializedContainerSource?
    abstract override val dispatchReceiverType: ConeSimpleKotlinType?
    abstract override val contextReceivers: List<FirContextReceiver>
    abstract override val controlFlowGraphReference: FirControlFlowGraphReference?
    abstract override val valueParameters: List<FirValueParameter>
    abstract override val annotations: List<FirAnnotation>
    abstract override val symbol: FirConstructorSymbol
    abstract val delegatedConstructor: FirDelegatedConstructorCall?
    abstract override val body: FirBlock?
    abstract val isPrimary: Boolean


    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    abstract override fun replaceTypeParameters(newTypeParameters: List<FirTypeParameterRef>)

    abstract override fun replaceStatus(newStatus: FirDeclarationStatus)

    abstract override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef)

    abstract override fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?)

    abstract override fun replaceDeprecation(newDeprecation: DeprecationsPerUseSite?)

    abstract override fun replaceContextReceivers(newContextReceivers: List<FirContextReceiver>)

    abstract override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?)

    abstract override fun replaceValueParameters(newValueParameters: List<FirValueParameter>)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract fun replaceDelegatedConstructor(newDelegatedConstructor: FirDelegatedConstructorCall?)

    abstract override fun replaceBody(newBody: FirBlock?)
}

inline fun <D> FirConstructor.transformTypeParameters(transformer: FirTransformer<D>, data: D): FirConstructor 
     = apply { replaceTypeParameters(typeParameters.transform(transformer, data)) }

inline fun <D> FirConstructor.transformStatus(transformer: FirTransformer<D>, data: D): FirConstructor 
     = apply { replaceStatus(status.transform(transformer, data)) }

inline fun <D> FirConstructor.transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirConstructor 
     = apply { replaceReturnTypeRef(returnTypeRef.transform(transformer, data)) }

inline fun <D> FirConstructor.transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirConstructor 
     = apply { replaceReceiverTypeRef(receiverTypeRef?.transform(transformer, data)) }

inline fun <D> FirConstructor.transformContextReceivers(transformer: FirTransformer<D>, data: D): FirConstructor 
     = apply { replaceContextReceivers(contextReceivers.transform(transformer, data)) }

inline fun <D> FirConstructor.transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirConstructor 
     = apply { replaceControlFlowGraphReference(controlFlowGraphReference?.transform(transformer, data)) }

inline fun <D> FirConstructor.transformValueParameters(transformer: FirTransformer<D>, data: D): FirConstructor 
     = apply { replaceValueParameters(valueParameters.transform(transformer, data)) }

inline fun <D> FirConstructor.transformAnnotations(transformer: FirTransformer<D>, data: D): FirConstructor 
     = apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirConstructor.transformDelegatedConstructor(transformer: FirTransformer<D>, data: D): FirConstructor 
     = apply { replaceDelegatedConstructor(delegatedConstructor?.transform(transformer, data)) }

inline fun <D> FirConstructor.transformBody(transformer: FirTransformer<D>, data: D): FirConstructor 
     = apply { replaceBody(body?.transform(transformer, data)) }
