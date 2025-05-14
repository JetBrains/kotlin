/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.MutableOrEmptyList
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirErrorTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

@OptIn(FirImplementationDetail::class, ResolveStateAccess::class)
internal class FirErrorFunctionImpl(
    override val source: KtSourceElement?,
    resolvePhase: FirResolvePhase,
    override var annotations: MutableOrEmptyList<FirAnnotation>,
    override val moduleData: FirModuleData,
    override val origin: FirDeclarationOrigin,
    override val attributes: FirDeclarationAttributes,
    override var staticReceiverParameter: FirTypeRef?,
    override var deprecationsProvider: DeprecationsProvider,
    override val containerSource: DeserializedContainerSource?,
    override val dispatchReceiverType: ConeSimpleKotlinType?,
    override var contextParameters: MutableOrEmptyList<FirValueParameter>,
    override val valueParameters: MutableList<FirValueParameter>,
    override val diagnostic: ConeDiagnostic,
    override val symbol: FirErrorFunctionSymbol,
) : FirErrorFunction() {
    override val typeParameters: List<FirTypeParameterRef>
        get() = emptyList()
    override var status: FirDeclarationStatus = FirResolvedDeclarationStatusImpl.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
    override var returnTypeRef: FirTypeRef = FirErrorTypeRefImpl(null, MutableOrEmptyList.empty(), null, null, diagnostic)
    override val receiverParameter: FirReceiverParameter?
        get() = null
    override var controlFlowGraphReference: FirControlFlowGraphReference? = null
    override val body: FirBlock?
        get() = null

    init {
        symbol.bind(this)
        resolveState = resolvePhase.asResolveState()
        @Suppress("SENSELESS_COMPARISON")
        require(source != null || origin != FirDeclarationOrigin.Source) { "${this::class.simpleName} with Source origin was instantiated without a source element." }
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        status.accept(visitor, data)
        returnTypeRef.accept(visitor, data)
        staticReceiverParameter?.accept(visitor, data)
        contextParameters.forEach { it.accept(visitor, data) }
        controlFlowGraphReference?.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirErrorFunctionImpl {
        transformAnnotations(transformer, data)
        transformStatus(transformer, data)
        transformReturnTypeRef(transformer, data)
        transformStaticReceiverParameter(transformer, data)
        transformContextParameters(transformer, data)
        controlFlowGraphReference = controlFlowGraphReference?.transform(transformer, data)
        transformValueParameters(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirErrorFunctionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirErrorFunctionImpl {
        return this
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirErrorFunctionImpl {
        status = status.transform(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirErrorFunctionImpl {
        returnTypeRef = returnTypeRef.transform(transformer, data)
        return this
    }

    override fun <D> transformReceiverParameter(transformer: FirTransformer<D>, data: D): FirErrorFunctionImpl {
        return this
    }

    override fun <D> transformStaticReceiverParameter(transformer: FirTransformer<D>, data: D): FirErrorFunctionImpl {
        staticReceiverParameter = staticReceiverParameter?.transform(transformer, data)
        return this
    }

    override fun <D> transformContextParameters(transformer: FirTransformer<D>, data: D): FirErrorFunctionImpl {
        contextParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirErrorFunctionImpl {
        valueParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformBody(transformer: FirTransformer<D>, data: D): FirErrorFunctionImpl {
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceStatus(newStatus: FirDeclarationStatus) {
        status = newStatus
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }

    override fun replaceReceiverParameter(newReceiverParameter: FirReceiverParameter?) {}

    override fun replaceStaticReceiverParameter(newStaticReceiverParameter: FirTypeRef?) {
        staticReceiverParameter = newStaticReceiverParameter
    }

    override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider) {
        deprecationsProvider = newDeprecationsProvider
    }

    override fun replaceContextParameters(newContextParameters: List<FirValueParameter>) {
        contextParameters = newContextParameters.toMutableOrEmpty()
    }

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {
        controlFlowGraphReference = newControlFlowGraphReference
    }

    override fun replaceValueParameters(newValueParameters: List<FirValueParameter>) {
        if (valueParameters === newValueParameters) return
        valueParameters.clear()
        valueParameters.addAll(newValueParameters)
    }

    override fun replaceBody(newBody: FirBlock?) {}
}
