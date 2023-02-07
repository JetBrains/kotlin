/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.synthetic

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.impl.FirEmptyContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirPropertyAccessorImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class FirSyntheticPropertyAccessor(
    val delegate: FirSimpleFunction,
    override val isGetter: Boolean,
    override val propertySymbol: FirPropertySymbol,
) : FirPropertyAccessor() {
    override val source: KtSourceElement?
        get() = delegate.source

    override val moduleData: FirModuleData
        get() = delegate.moduleData

    override val origin: FirDeclarationOrigin
        get() = FirDeclarationOrigin.Synthetic

    override val returnTypeRef: FirTypeRef
        get() = delegate.returnTypeRef

    override val resolvePhase: FirResolvePhase
        get() = delegate.resolvePhase

    override val status: FirDeclarationStatus
        get() = delegate.status

    override val dispatchReceiverType: ConeSimpleKotlinType?
        get() = delegate.dispatchReceiverType

    override val receiverParameter: FirReceiverParameter?
        get() = null
    
    override val deprecationsProvider: DeprecationsProvider
        get() = delegate.deprecationsProvider

    override val valueParameters: List<FirValueParameter>
        get() = delegate.valueParameters

    override val annotations: List<FirAnnotation>
        get() = delegate.annotations

    override val typeParameters: List<FirTypeParameter>
        get() = emptyList()

    override val isSetter: Boolean
        get() = !isGetter

    override val body: FirBlock?
        get() = delegate.body

    override val attributes: FirDeclarationAttributes
        get() = delegate.attributes

    override val symbol: FirPropertyAccessorSymbol = FirPropertyAccessorSymbol().apply {
        bind(this@FirSyntheticPropertyAccessor)
    }

    override val contextReceivers: List<FirContextReceiver>
        get() = emptyList()

    override val controlFlowGraphReference: FirControlFlowGraphReference? = null

    override val contractDescription: FirContractDescription = FirEmptyContractDescription

    override val containerSource: DeserializedContainerSource? get() = null

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        delegate.accept(visitor, data)
        controlFlowGraphReference?.accept(visitor, data)
        contractDescription.accept(visitor, data)
    }

    override fun replaceBody(newBody: FirBlock?) {
        notSupported()
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirPropertyAccessorImpl {
        notSupported()
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirPropertyAccessorImpl {
        notSupported()
    }

    override fun <D> transformReceiverParameter(transformer: FirTransformer<D>, data: D): FirPropertyAccessorImpl {
        notSupported()
    }

    override fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirPropertyAccessorImpl {
        notSupported()
    }

    override fun <D> transformContractDescription(transformer: FirTransformer<D>, data: D): FirPropertyAccessorImpl {
        notSupported()
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirPropertyAccessorImpl {
        notSupported()
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirPropertyAccessor {
        notSupported()
    }

    override fun <D> transformBody(transformer: FirTransformer<D>, data: D): FirPropertyAccessor {
        notSupported()
    }

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirPropertyAccessor {
        notSupported()
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        notSupported()
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef) {
        notSupported()
    }

    override fun replaceReceiverParameter(newReceiverParameter: FirReceiverParameter?) {
        notSupported()
    }

    override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider) {
        notSupported()
    }

    override fun replaceValueParameters(newValueParameters: List<FirValueParameter>) {
        notSupported()
    }

    override fun replaceContractDescription(newContractDescription: FirContractDescription) {
        notSupported()
    }

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {
        notSupported()
    }

    override fun replaceContextReceivers(newContextReceivers: List<FirContextReceiver>) {
        notSupported()
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        notSupported()
    }

    override fun replaceStatus(newStatus: FirDeclarationStatus) {
        notSupported()
    }

    private fun notSupported(): Nothing {
        error("Mutation of synthetic property accessor isn't supported")
    }
}
