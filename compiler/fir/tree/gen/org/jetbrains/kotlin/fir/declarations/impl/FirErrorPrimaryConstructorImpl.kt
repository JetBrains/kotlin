/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.declarations.DeprecationsProvider
import org.jetbrains.kotlin.fir.declarations.FirContextReceiver
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirErrorPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.FirReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirResolveState
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.asResolveState
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.fir.MutableOrEmptyList
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.declarations.ResolveStateAccess

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirErrorPrimaryConstructorImpl @FirImplementationDetail constructor(
    override val source: KtSourceElement?,
    resolvePhase: FirResolvePhase,
    override val moduleData: FirModuleData,
    override val origin: FirDeclarationOrigin,
    override val attributes: FirDeclarationAttributes,
    override val typeParameters: MutableList<FirTypeParameterRef>,
    override var status: FirDeclarationStatus,
    override var returnTypeRef: FirTypeRef,
    override var receiverParameter: FirReceiverParameter?,
    override var deprecationsProvider: DeprecationsProvider,
    override val containerSource: DeserializedContainerSource?,
    override val dispatchReceiverType: ConeSimpleKotlinType?,
    override var contextReceivers: MutableOrEmptyList<FirContextReceiver>,
    override val valueParameters: MutableList<FirValueParameter>,
    override var contractDescription: FirContractDescription,
    override var annotations: MutableOrEmptyList<FirAnnotation>,
    override val symbol: FirConstructorSymbol,
    override var delegatedConstructor: FirDelegatedConstructorCall?,
    override var body: FirBlock?,
    override val diagnostic: ConeDiagnostic,
) : FirErrorPrimaryConstructor() {
    override var controlFlowGraphReference: FirControlFlowGraphReference? = null
    override val isPrimary: Boolean get() = true

    init {
        symbol.bind(this)
        @OptIn(ResolveStateAccess::class)
        resolveState = resolvePhase.asResolveState()
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        typeParameters.forEach { it.accept(visitor, data) }
        status.accept(visitor, data)
        returnTypeRef.accept(visitor, data)
        receiverParameter?.accept(visitor, data)
        contextReceivers.forEach { it.accept(visitor, data) }
        controlFlowGraphReference?.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }
        contractDescription.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        delegatedConstructor?.accept(visitor, data)
        body?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirErrorPrimaryConstructorImpl {
        transformTypeParameters(transformer, data)
        transformStatus(transformer, data)
        transformReturnTypeRef(transformer, data)
        transformReceiverParameter(transformer, data)
        contextReceivers.transformInplace(transformer, data)
        controlFlowGraphReference = controlFlowGraphReference?.transform(transformer, data)
        transformValueParameters(transformer, data)
        transformContractDescription(transformer, data)
        transformAnnotations(transformer, data)
        transformDelegatedConstructor(transformer, data)
        transformBody(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirErrorPrimaryConstructorImpl {
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirErrorPrimaryConstructorImpl {
        status = status.transform(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirErrorPrimaryConstructorImpl {
        returnTypeRef = returnTypeRef.transform(transformer, data)
        return this
    }

    override fun <D> transformReceiverParameter(transformer: FirTransformer<D>, data: D): FirErrorPrimaryConstructorImpl {
        receiverParameter = receiverParameter?.transform(transformer, data)
        return this
    }

    override fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirErrorPrimaryConstructorImpl {
        valueParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformContractDescription(transformer: FirTransformer<D>, data: D): FirErrorPrimaryConstructorImpl {
        contractDescription = contractDescription.transform(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirErrorPrimaryConstructorImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformDelegatedConstructor(transformer: FirTransformer<D>, data: D): FirErrorPrimaryConstructorImpl {
        delegatedConstructor = delegatedConstructor?.transform(transformer, data)
        return this
    }

    override fun <D> transformBody(transformer: FirTransformer<D>, data: D): FirErrorPrimaryConstructorImpl {
        body = body?.transform(transformer, data)
        return this
    }

    override fun replaceStatus(newStatus: FirDeclarationStatus) {
        status = newStatus
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }

    override fun replaceReceiverParameter(newReceiverParameter: FirReceiverParameter?) {
        receiverParameter = newReceiverParameter
    }

    override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider) {
        deprecationsProvider = newDeprecationsProvider
    }

    override fun replaceContextReceivers(newContextReceivers: List<FirContextReceiver>) {
        contextReceivers = newContextReceivers.toMutableOrEmpty()
    }

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {
        controlFlowGraphReference = newControlFlowGraphReference
    }

    override fun replaceValueParameters(newValueParameters: List<FirValueParameter>) {
        valueParameters.clear()
        valueParameters.addAll(newValueParameters)
    }

    override fun replaceContractDescription(newContractDescription: FirContractDescription) {
        contractDescription = newContractDescription
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceDelegatedConstructor(newDelegatedConstructor: FirDelegatedConstructorCall?) {
        delegatedConstructor = newDelegatedConstructor
    }

    override fun replaceBody(newBody: FirBlock?) {
        body = newBody
    }
}
