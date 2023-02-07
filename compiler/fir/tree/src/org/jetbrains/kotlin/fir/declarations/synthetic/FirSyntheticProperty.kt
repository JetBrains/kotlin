/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.synthetic

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class FirSyntheticProperty(
    override val moduleData: FirModuleData,
    override val name: Name,
    override val isVar: Boolean,
    override val symbol: FirSyntheticPropertySymbol,
    override val status: FirDeclarationStatus,
    override var resolvePhase: FirResolvePhase,
    override val getter: FirSyntheticPropertyAccessor,
    override val setter: FirSyntheticPropertyAccessor? = null,
    override val backingField: FirBackingField? = null,
    override val deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider
) : FirProperty() {
    init {
        symbol.bind(this)
    }

    override val returnTypeRef: FirTypeRef
        get() = getter.returnTypeRef

    override val dispatchReceiverType: ConeSimpleKotlinType?
        get() = getter.dispatchReceiverType

    override val source: KtSourceElement?
        get() = null

    override val origin: FirDeclarationOrigin
        get() = FirDeclarationOrigin.Synthetic

    override val initializer: FirExpression?
        get() = null

    override val delegate: FirExpression?
        get() = null

    override val delegateFieldSymbol: FirDelegateFieldSymbol?
        get() = null

    override val isLocal: Boolean
        get() = false

    override val receiverParameter: FirReceiverParameter?
        get() = null

    override val isVal: Boolean
        get() = !isVar

    override val annotations: List<FirAnnotation>
        get() = emptyList()

    override val typeParameters: List<FirTypeParameter>
        get() = emptyList()

    override val containerSource: DeserializedContainerSource?
        get() = null

    override val controlFlowGraphReference: FirControlFlowGraphReference? = null

    override val attributes: FirDeclarationAttributes = FirDeclarationAttributes()

    override val bodyResolveState: FirPropertyBodyResolveState
        get() = FirPropertyBodyResolveState.EVERYTHING_RESOLVED

    override val contextReceivers: List<FirContextReceiver>
        get() = emptyList()

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        returnTypeRef.accept(visitor, data)
        status.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirSyntheticProperty {
        notSupported()
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirSyntheticProperty {
        notSupported()
    }

    override fun <D> transformReceiverParameter(transformer: FirTransformer<D>, data: D): FirSyntheticProperty {
        notSupported()
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirSyntheticProperty {
        notSupported()
    }

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirSyntheticProperty {
        notSupported()
    }

    override fun <D> transformInitializer(transformer: FirTransformer<D>, data: D): FirSyntheticProperty {
        notSupported()
    }

    override fun <D> transformGetter(transformer: FirTransformer<D>, data: D): FirSyntheticProperty {
        notSupported()
    }

    override fun <D> transformSetter(transformer: FirTransformer<D>, data: D): FirSyntheticProperty {
        notSupported()
    }

    override fun <D> transformBackingField(transformer: FirTransformer<D>, data: D): FirSyntheticProperty {
        notSupported()
    }

    override fun <D> transformDelegate(transformer: FirTransformer<D>, data: D): FirProperty {
        notSupported()
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirSyntheticProperty {
        notSupported()
    }

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirProperty {
        notSupported()
    }

    override fun <D> transformContextReceivers(transformer: FirTransformer<D>, data: D): FirProperty {
        notSupported()
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
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

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {
        notSupported()
    }

    override fun replaceInitializer(newInitializer: FirExpression?) {
        notSupported()
    }

    override fun replaceBodyResolveState(newBodyResolveState: FirPropertyBodyResolveState) {
        notSupported()
    }

    override fun replaceGetter(newGetter: FirPropertyAccessor?) {
        notSupported()
    }

    override fun replaceSetter(newSetter: FirPropertyAccessor?) {
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
        error("Transformation of synthetic property isn't supported")
    }
}
