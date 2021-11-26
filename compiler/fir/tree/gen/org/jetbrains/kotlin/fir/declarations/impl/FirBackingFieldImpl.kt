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
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

open class FirBackingFieldImpl @FirImplementationDetail constructor(
    override val source: KtSourceElement?,
    override val moduleData: FirModuleData,
    @Volatile
    override var resolvePhase: FirResolvePhase,
    override val origin: FirDeclarationOrigin,
    override val attributes: FirDeclarationAttributes,
    override var returnTypeRef: FirTypeRef,
    override var receiverTypeRef: FirTypeRef?,
    override var deprecation: DeprecationsPerUseSite?,
    override val containerSource: DeserializedContainerSource?,
    override val dispatchReceiverType: ConeKotlinType?,
    override val name: Name,
    override var delegate: FirExpression?,
    override val isVar: Boolean,
    override val isVal: Boolean,
    override var getter: FirPropertyAccessor?,
    override var setter: FirPropertyAccessor?,
    override var backingField: FirBackingField?,
    override val symbol: FirBackingFieldSymbol,
    override val propertySymbol: FirPropertySymbol,
    override var initializer: FirExpression?,
    override val annotations: MutableList<FirAnnotation>,
    override val typeParameters: MutableList<FirTypeParameter>,
    override var status: FirDeclarationStatus,
) : FirBackingField() {
    init {
        symbol.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        returnTypeRef.accept(visitor, data)
        receiverTypeRef?.accept(visitor, data)
        delegate?.accept(visitor, data)
        getter?.accept(visitor, data)
        setter?.accept(visitor, data)
        backingField?.accept(visitor, data)
        initializer?.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
        status.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirBackingFieldImpl {
        transformReturnTypeRef(transformer, data)
        transformReceiverTypeRef(transformer, data)
        transformDelegate(transformer, data)
        transformGetter(transformer, data)
        transformSetter(transformer, data)
        transformBackingField(transformer, data)
        transformInitializer(transformer, data)
        transformTypeParameters(transformer, data)
        transformStatus(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirBackingFieldImpl {
        returnTypeRef = returnTypeRef.transform(transformer, data)
        return this
    }

    override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirBackingFieldImpl {
        receiverTypeRef = receiverTypeRef?.transform(transformer, data)
        return this
    }

    override fun <D> transformDelegate(transformer: FirTransformer<D>, data: D): FirBackingFieldImpl {
        delegate = delegate?.transform(transformer, data)
        return this
    }

    override fun <D> transformGetter(transformer: FirTransformer<D>, data: D): FirBackingFieldImpl {
        getter = getter?.transform(transformer, data)
        return this
    }

    override fun <D> transformSetter(transformer: FirTransformer<D>, data: D): FirBackingFieldImpl {
        setter = setter?.transform(transformer, data)
        return this
    }

    override fun <D> transformBackingField(transformer: FirTransformer<D>, data: D): FirBackingFieldImpl {
        backingField = backingField?.transform(transformer, data)
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
        typeParameters.transformInplace(transformer, data)
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
        initializer = newInitializer
    }
}
