/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.DeprecationsPerUseSite
import org.jetbrains.kotlin.fir.declarations.FirBackingField
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirDelegateField
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

open class FirDelegateFieldImpl @FirImplementationDetail constructor(
    override val source: KtSourceElement?,
    override val moduleData: FirModuleData,
    @Volatile
    override var resolvePhase: FirResolvePhase,
    override val origin: FirDeclarationOrigin,
    override val attributes: FirDeclarationAttributes,
    override val typeParameters: MutableList<FirTypeParameterRef>,
    override var returnTypeRef: FirTypeRef,
    override var receiverTypeRef: FirTypeRef?,
    override var deprecation: DeprecationsPerUseSite?,
    override val containerSource: DeserializedContainerSource?,
    override val dispatchReceiverType: ConeSimpleKotlinType?,
    override val name: Name,
    override val isVar: Boolean,
    override val isVal: Boolean,
    override var getter: FirPropertyAccessor?,
    override var setter: FirPropertyAccessor?,
    override var backingField: FirBackingField?,
    override var delegateField: FirDelegateField?,
    override val annotations: MutableList<FirAnnotation>,
    override val symbol: FirDelegateFieldSymbol,
    override val propertySymbol: FirPropertySymbol,
    override var initializer: FirExpression,
    override var status: FirDeclarationStatus,
) : FirDelegateField() {
    init {
        symbol.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        typeParameters.forEach { it.accept(visitor, data) }
        returnTypeRef.accept(visitor, data)
        receiverTypeRef?.accept(visitor, data)
        getter?.accept(visitor, data)
        setter?.accept(visitor, data)
        backingField?.accept(visitor, data)
        delegateField?.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        initializer.accept(visitor, data)
        status.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirDelegateFieldImpl {
        transformTypeParameters(transformer, data)
        transformReturnTypeRef(transformer, data)
        transformReceiverTypeRef(transformer, data)
        transformGetter(transformer, data)
        transformSetter(transformer, data)
        transformBackingField(transformer, data)
        transformDelegateField(transformer, data)
        transformInitializer(transformer, data)
        transformStatus(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirDelegateFieldImpl {
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirDelegateFieldImpl {
        returnTypeRef = returnTypeRef.transform(transformer, data)
        return this
    }

    override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirDelegateFieldImpl {
        receiverTypeRef = receiverTypeRef?.transform(transformer, data)
        return this
    }

    override fun <D> transformGetter(transformer: FirTransformer<D>, data: D): FirDelegateFieldImpl {
        getter = getter?.transform(transformer, data)
        return this
    }

    override fun <D> transformSetter(transformer: FirTransformer<D>, data: D): FirDelegateFieldImpl {
        setter = setter?.transform(transformer, data)
        return this
    }

    override fun <D> transformBackingField(transformer: FirTransformer<D>, data: D): FirDelegateFieldImpl {
        backingField = backingField?.transform(transformer, data)
        return this
    }

    override fun <D> transformDelegateField(transformer: FirTransformer<D>, data: D): FirDelegateFieldImpl {
        delegateField = delegateField?.transform(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirDelegateFieldImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformInitializer(transformer: FirTransformer<D>, data: D): FirDelegateFieldImpl {
        initializer = initializer.transform(transformer, data)
        return this
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirDelegateFieldImpl {
        status = status.transform(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirDelegateFieldImpl {
        transformAnnotations(transformer, data)
        return this
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }

    override fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?) {
        receiverTypeRef = newReceiverTypeRef
    }

    override fun replaceDeprecation(newDeprecation: DeprecationsPerUseSite?) {
        deprecation = newDeprecation
    }

    override fun replaceGetter(newGetter: FirPropertyAccessor?) {
        getter = newGetter
    }

    override fun replaceSetter(newSetter: FirPropertyAccessor?) {
        setter = newSetter
    }

    override fun replaceInitializer(newInitializer: FirExpression?) {
        require(newInitializer != null)
        initializer = newInitializer
    }
}
