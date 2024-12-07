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
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

@OptIn(FirImplementationDetail::class, ResolveStateAccess::class)
open class FirBackingFieldImpl @FirImplementationDetail constructor(
    override val source: KtSourceElement?,
    resolvePhase: FirResolvePhase,
    override val moduleData: FirModuleData,
    override val origin: FirDeclarationOrigin,
    override val attributes: FirDeclarationAttributes,
    override var returnTypeRef: FirTypeRef,
    override var deprecationsProvider: DeprecationsProvider,
    override val name: Name,
    override val isVar: Boolean,
    override val isVal: Boolean,
    override val symbol: FirBackingFieldSymbol,
    override val propertySymbol: FirPropertySymbol,
    override var initializer: FirExpression?,
    override var annotations: MutableOrEmptyList<FirAnnotation>,
    override var status: FirDeclarationStatus,
) : FirBackingField() {
    override val receiverParameter: FirReceiverParameter?
        get() = null
    override val containerSource: DeserializedContainerSource?
        get() = null
    override val dispatchReceiverType: ConeSimpleKotlinType?
        get() = null
    override val contextParameters: List<FirValueParameter>
        get() = emptyList()
    override val delegate: FirExpression?
        get() = null
    override val getter: FirPropertyAccessor?
        get() = null
    override val setter: FirPropertyAccessor?
        get() = null
    override val backingField: FirBackingField?
        get() = null
    override val typeParameters: List<FirTypeParameter>
        get() = emptyList()

    init {
        symbol.bind(this)
        resolveState = resolvePhase.asResolveState()
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        returnTypeRef.accept(visitor, data)
        initializer?.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        status.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirBackingFieldImpl {
        transformReturnTypeRef(transformer, data)
        transformInitializer(transformer, data)
        transformStatus(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirBackingFieldImpl {
        returnTypeRef = returnTypeRef.transform(transformer, data)
        return this
    }

    override fun <D> transformReceiverParameter(transformer: FirTransformer<D>, data: D): FirBackingFieldImpl {
        return this
    }

    override fun <D> transformContextParameters(transformer: FirTransformer<D>, data: D): FirBackingFieldImpl {
        return this
    }

    override fun <D> transformDelegate(transformer: FirTransformer<D>, data: D): FirBackingFieldImpl {
        return this
    }

    override fun <D> transformGetter(transformer: FirTransformer<D>, data: D): FirBackingFieldImpl {
        return this
    }

    override fun <D> transformSetter(transformer: FirTransformer<D>, data: D): FirBackingFieldImpl {
        return this
    }

    override fun <D> transformBackingField(transformer: FirTransformer<D>, data: D): FirBackingFieldImpl {
        return this
    }

    override fun <D> transformInitializer(transformer: FirTransformer<D>, data: D): FirBackingFieldImpl {
        initializer = initializer?.transform(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirBackingFieldImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirBackingFieldImpl {
        return this
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirBackingFieldImpl {
        status = status.transform(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirBackingFieldImpl {
        transformAnnotations(transformer, data)
        return this
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }

    override fun replaceReceiverParameter(newReceiverParameter: FirReceiverParameter?) {}

    override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider) {
        deprecationsProvider = newDeprecationsProvider
    }

    override fun replaceContextParameters(newContextParameters: List<FirValueParameter>) {}

    override fun replaceDelegate(newDelegate: FirExpression?) {}

    override fun replaceGetter(newGetter: FirPropertyAccessor?) {}

    override fun replaceSetter(newSetter: FirPropertyAccessor?) {}

    override fun replaceInitializer(newInitializer: FirExpression?) {
        initializer = newInitializer
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceStatus(newStatus: FirDeclarationStatus) {
        status = newStatus
    }
}
