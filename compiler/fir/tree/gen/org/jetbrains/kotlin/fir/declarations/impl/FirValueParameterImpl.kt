/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirValueParameterImpl(
    override val source: KtSourceElement?,
    override val moduleData: FirModuleData,
    @Volatile
    override var resolvePhase: FirResolvePhase,
    override val origin: FirDeclarationOrigin,
    override val attributes: FirDeclarationAttributes,
    override var returnTypeRef: FirTypeRef,
    override var deprecationsProvider: DeprecationsProvider,
    override val containerSource: DeserializedContainerSource?,
    override val dispatchReceiverType: ConeSimpleKotlinType?,
    override val contextReceivers: MutableList<FirContextReceiver>,
    override val name: Name,
    override var backingField: FirBackingField?,
    override val annotations: MutableList<FirAnnotation>,
    override val symbol: FirValueParameterSymbol,
    override var defaultValue: FirExpression?,
    override val isCrossinline: Boolean,
    override val isNoinline: Boolean,
    override val isVararg: Boolean,
) : FirValueParameter() {
    override val typeParameters: List<FirTypeParameterRef> get() = emptyList()
    override var status: FirDeclarationStatus = FirResolvedDeclarationStatusImpl.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
    override val receiverTypeRef: FirTypeRef? get() = null
    override val initializer: FirExpression? get() = null
    override val delegate: FirExpression? get() = null
    override val isVar: Boolean get() = false
    override val isVal: Boolean get() = true
    override val getter: FirPropertyAccessor? get() = null
    override val setter: FirPropertyAccessor? get() = null
    override var controlFlowGraphReference: FirControlFlowGraphReference? = null

    init {
        symbol.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        status.accept(visitor, data)
        returnTypeRef.accept(visitor, data)
        contextReceivers.forEach { it.accept(visitor, data) }
        backingField?.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        controlFlowGraphReference?.accept(visitor, data)
        defaultValue?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirValueParameterImpl {
        transformStatus(transformer, data)
        transformReturnTypeRef(transformer, data)
        transformBackingField(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirValueParameterImpl {
        return this
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirValueParameterImpl {
        status = status.transform(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirValueParameterImpl {
        returnTypeRef = returnTypeRef.transform(transformer, data)
        return this
    }

    override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirValueParameterImpl {
        return this
    }

    override fun <D> transformInitializer(transformer: FirTransformer<D>, data: D): FirValueParameterImpl {
        return this
    }

    override fun <D> transformDelegate(transformer: FirTransformer<D>, data: D): FirValueParameterImpl {
        return this
    }

    override fun <D> transformGetter(transformer: FirTransformer<D>, data: D): FirValueParameterImpl {
        return this
    }

    override fun <D> transformSetter(transformer: FirTransformer<D>, data: D): FirValueParameterImpl {
        return this
    }

    override fun <D> transformBackingField(transformer: FirTransformer<D>, data: D): FirValueParameterImpl {
        backingField = backingField?.transform(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirValueParameterImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirValueParameterImpl {
        contextReceivers.transformInplace(transformer, data)
        transformAnnotations(transformer, data)
        controlFlowGraphReference = controlFlowGraphReference?.transform(transformer, data)
        defaultValue = defaultValue?.transform(transformer, data)
        return this
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }

    override fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?) {}

    override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider) {
        deprecationsProvider = newDeprecationsProvider
    }

    override fun replaceContextReceivers(newContextReceivers: List<FirContextReceiver>) {
        contextReceivers.clear()
        contextReceivers.addAll(newContextReceivers)
    }

    override fun replaceInitializer(newInitializer: FirExpression?) {}

    override fun replaceGetter(newGetter: FirPropertyAccessor?) {}

    override fun replaceSetter(newSetter: FirPropertyAccessor?) {}

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {
        controlFlowGraphReference = newControlFlowGraphReference
    }
}
