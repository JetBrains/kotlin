/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirTargetElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

sealed class FirFunction : FirCallableDeclaration(), FirTargetElement, FirControlFlowGraphOwner, FirStatement {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
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
    abstract override val symbol: FirFunctionSymbol<out FirFunction>
    abstract val valueParameters: List<FirValueParameter>
    abstract val body: FirBlock?


    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    abstract override fun replaceTypeParameters(newTypeParameters: List<FirTypeParameterRef>)

    abstract override fun replaceStatus(newStatus: FirDeclarationStatus)

    abstract override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef)

    abstract override fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?)

    abstract override fun replaceDeprecation(newDeprecation: DeprecationsPerUseSite?)

    abstract override fun replaceContextReceivers(newContextReceivers: List<FirContextReceiver>)

    abstract override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?)

    abstract fun replaceValueParameters(newValueParameters: List<FirValueParameter>)

    abstract fun replaceBody(newBody: FirBlock?)
}

inline fun <D> FirFunction.transformAnnotations(transformer: FirTransformer<D>, data: D): FirFunction 
     = apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirFunction.transformTypeParameters(transformer: FirTransformer<D>, data: D): FirFunction 
     = apply { replaceTypeParameters(typeParameters.transform(transformer, data)) }

inline fun <D> FirFunction.transformStatus(transformer: FirTransformer<D>, data: D): FirFunction 
     = apply { replaceStatus(status.transform(transformer, data)) }

inline fun <D> FirFunction.transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirFunction 
     = apply { replaceReturnTypeRef(returnTypeRef.transform(transformer, data)) }

inline fun <D> FirFunction.transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirFunction 
     = apply { replaceReceiverTypeRef(receiverTypeRef?.transform(transformer, data)) }

inline fun <D> FirFunction.transformContextReceivers(transformer: FirTransformer<D>, data: D): FirFunction 
     = apply { replaceContextReceivers(contextReceivers.transform(transformer, data)) }

inline fun <D> FirFunction.transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirFunction 
     = apply { replaceControlFlowGraphReference(controlFlowGraphReference?.transform(transformer, data)) }

inline fun <D> FirFunction.transformValueParameters(transformer: FirTransformer<D>, data: D): FirFunction 
     = apply { replaceValueParameters(valueParameters.transform(transformer, data)) }

inline fun <D> FirFunction.transformBody(transformer: FirTransformer<D>, data: D): FirFunction 
     = apply { replaceBody(body?.transform(transformer, data)) }
