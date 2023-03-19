/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.DeprecationsProvider
import org.jetbrains.kotlin.fir.declarations.FirBackingField
import org.jetbrains.kotlin.fir.declarations.FirContextReceiver
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirPropertyBodyResolveState
import org.jetbrains.kotlin.fir.declarations.FirReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.fir.MutableOrEmptyList
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirPropertyImpl(
    override val source: KtSourceElement?,
    @Volatile
    override var resolvePhase: FirResolvePhase,
    override val moduleData: FirModuleData,
    override val origin: FirDeclarationOrigin,
    override val attributes: FirDeclarationAttributes,
    override var status: FirDeclarationStatus,
    override var returnTypeRef: FirTypeRef,
    override var receiverParameter: FirReceiverParameter?,
    override var deprecationsProvider: DeprecationsProvider,
    override val containerSource: DeserializedContainerSource?,
    override val dispatchReceiverType: ConeSimpleKotlinType?,
    override val name: Name,
    override var initializer: FirExpression?,
    override var delegate: FirExpression?,
    override val isVar: Boolean,
    override var getter: FirPropertyAccessor?,
    override var setter: FirPropertyAccessor?,
    override var backingField: FirBackingField?,
    override var annotations: MutableOrEmptyList<FirAnnotation>,
    override var contextReceivers: MutableOrEmptyList<FirContextReceiver>,
    override val symbol: FirPropertySymbol,
    override val delegateFieldSymbol: FirDelegateFieldSymbol?,
    override val isLocal: Boolean,
    override var bodyResolveState: FirPropertyBodyResolveState,
    override val typeParameters: MutableList<FirTypeParameter>,
) : FirProperty() {
    override val isVal: Boolean get() = !isVar
    override var controlFlowGraphReference: FirControlFlowGraphReference? = null

    init {
        symbol.bind(this)
        delegateFieldSymbol?.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        status.accept(visitor, data)
        returnTypeRef.accept(visitor, data)
        receiverParameter?.accept(visitor, data)
        initializer?.accept(visitor, data)
        delegate?.accept(visitor, data)
        getter?.accept(visitor, data)
        setter?.accept(visitor, data)
        backingField?.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        controlFlowGraphReference?.accept(visitor, data)
        contextReceivers.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirPropertyImpl {
        transformStatus(transformer, data)
        transformReturnTypeRef(transformer, data)
        transformReceiverParameter(transformer, data)
        transformInitializer(transformer, data)
        transformDelegate(transformer, data)
        transformGetter(transformer, data)
        transformSetter(transformer, data)
        transformBackingField(transformer, data)
        transformContextReceivers(transformer, data)
        transformTypeParameters(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirPropertyImpl {
        status = status.transform(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirPropertyImpl {
        returnTypeRef = returnTypeRef.transform(transformer, data)
        return this
    }

    override fun <D> transformReceiverParameter(transformer: FirTransformer<D>, data: D): FirPropertyImpl {
        receiverParameter = receiverParameter?.transform(transformer, data)
        return this
    }

    override fun <D> transformInitializer(transformer: FirTransformer<D>, data: D): FirPropertyImpl {
        initializer = initializer?.transform(transformer, data)
        return this
    }

    override fun <D> transformDelegate(transformer: FirTransformer<D>, data: D): FirPropertyImpl {
        delegate = delegate?.transform(transformer, data)
        return this
    }

    override fun <D> transformGetter(transformer: FirTransformer<D>, data: D): FirPropertyImpl {
        getter = getter?.transform(transformer, data)
        return this
    }

    override fun <D> transformSetter(transformer: FirTransformer<D>, data: D): FirPropertyImpl {
        setter = setter?.transform(transformer, data)
        return this
    }

    override fun <D> transformBackingField(transformer: FirTransformer<D>, data: D): FirPropertyImpl {
        backingField = backingField?.transform(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirPropertyImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformContextReceivers(transformer: FirTransformer<D>, data: D): FirPropertyImpl {
        contextReceivers.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirPropertyImpl {
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirPropertyImpl {
        transformAnnotations(transformer, data)
        controlFlowGraphReference = controlFlowGraphReference?.transform(transformer, data)
        return this
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
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

    override fun replaceInitializer(newInitializer: FirExpression?) {
        initializer = newInitializer
    }

    override fun replaceGetter(newGetter: FirPropertyAccessor?) {
        getter = newGetter
    }

    override fun replaceSetter(newSetter: FirPropertyAccessor?) {
        setter = newSetter
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {
        controlFlowGraphReference = newControlFlowGraphReference
    }

    override fun replaceContextReceivers(newContextReceivers: List<FirContextReceiver>) {
        contextReceivers = newContextReceivers.toMutableOrEmpty()
    }

    override fun replaceBodyResolveState(newBodyResolveState: FirPropertyBodyResolveState) {
        bodyResolveState = newBodyResolveState
    }
}
