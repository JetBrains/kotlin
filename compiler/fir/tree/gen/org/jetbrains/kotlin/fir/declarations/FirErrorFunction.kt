/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")
package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirErrorFunction : FirFunction(), FirDiagnosticHolder {
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
    abstract override val diagnostic: ConeDiagnostic
    abstract override val symbol: FirErrorFunctionSymbol
    abstract override val typeParameters: List<FirTypeParameterRef>


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

    abstract override fun replaceTypeParameters(newTypeParameters: List<FirTypeParameterRef>)
}

inline fun <D> FirErrorFunction.transformAnnotations(transformer: FirTransformer<D>, data: D): FirErrorFunction  = 
    apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirErrorFunction.transformStatus(transformer: FirTransformer<D>, data: D): FirErrorFunction  = 
    apply { replaceStatus(status.transform(transformer, data)) }

inline fun <D> FirErrorFunction.transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirErrorFunction  = 
    apply { replaceReturnTypeRef(returnTypeRef.transform(transformer, data)) }

inline fun <D> FirErrorFunction.transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirErrorFunction  = 
    apply { replaceReceiverTypeRef(receiverTypeRef?.transform(transformer, data)) }

inline fun <D> FirErrorFunction.transformContextReceivers(transformer: FirTransformer<D>, data: D): FirErrorFunction  = 
    apply { replaceContextReceivers(contextReceivers.transform(transformer, data)) }

inline fun <D> FirErrorFunction.transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirErrorFunction  = 
    apply { replaceControlFlowGraphReference(controlFlowGraphReference?.transform(transformer, data)) }

inline fun <D> FirErrorFunction.transformValueParameters(transformer: FirTransformer<D>, data: D): FirErrorFunction  = 
    apply { replaceValueParameters(valueParameters.transform(transformer, data)) }

inline fun <D> FirErrorFunction.transformBody(transformer: FirTransformer<D>, data: D): FirErrorFunction  = 
    apply { replaceBody(body?.transform(transformer, data)) }

inline fun <D> FirErrorFunction.transformTypeParameters(transformer: FirTransformer<D>, data: D): FirErrorFunction  = 
    apply { replaceTypeParameters(typeParameters.transform(transformer, data)) }
