/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")
package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

sealed class FirVariable : FirCallableDeclaration(), FirStatement {
    abstract override val source: KtSourceElement?
    abstract override val moduleData: FirModuleData
    abstract override val resolvePhase: FirResolvePhase
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val typeParameters: List<FirTypeParameterRef>
    abstract override val status: FirDeclarationStatus
    abstract override val returnTypeRef: FirTypeRef
    abstract override val receiverTypeRef: FirTypeRef?
    abstract override val deprecation: DeprecationsPerUseSite?
    abstract override val containerSource: DeserializedContainerSource?
    abstract override val dispatchReceiverType: ConeSimpleKotlinType?
    abstract override val contextReceivers: List<FirContextReceiver>
    abstract val name: Name
    abstract override val symbol: FirVariableSymbol<out FirVariable>
    abstract val initializer: FirExpression?
    abstract val delegate: FirExpression?
    abstract val isVar: Boolean
    abstract val isVal: Boolean
    abstract val getter: FirPropertyAccessor?
    abstract val setter: FirPropertyAccessor?
    abstract val backingField: FirBackingField?
    abstract override val annotations: List<FirAnnotation>


    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    abstract override fun replaceTypeParameters(newTypeParameters: List<FirTypeParameterRef>)

    abstract override fun replaceStatus(newStatus: FirDeclarationStatus)

    abstract override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef)

    abstract override fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?)

    abstract override fun replaceDeprecation(newDeprecation: DeprecationsPerUseSite?)

    abstract override fun replaceContextReceivers(newContextReceivers: List<FirContextReceiver>)

    abstract fun replaceInitializer(newInitializer: FirExpression?)

    abstract fun replaceDelegate(newDelegate: FirExpression?)

    abstract fun replaceGetter(newGetter: FirPropertyAccessor?)

    abstract fun replaceSetter(newSetter: FirPropertyAccessor?)

    abstract fun replaceBackingField(newBackingField: FirBackingField?)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)
}

inline fun <D> FirVariable.transformTypeParameters(transformer: FirTransformer<D>, data: D): FirVariable  = 
    apply { replaceTypeParameters(typeParameters.transform(transformer, data)) }

inline fun <D> FirVariable.transformStatus(transformer: FirTransformer<D>, data: D): FirVariable  = 
    apply { replaceStatus(status.transform(transformer, data)) }

inline fun <D> FirVariable.transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirVariable  = 
    apply { replaceReturnTypeRef(returnTypeRef.transform(transformer, data)) }

inline fun <D> FirVariable.transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirVariable  = 
    apply { replaceReceiverTypeRef(receiverTypeRef?.transform(transformer, data)) }

inline fun <D> FirVariable.transformContextReceivers(transformer: FirTransformer<D>, data: D): FirVariable  = 
    apply { replaceContextReceivers(contextReceivers.transform(transformer, data)) }

inline fun <D> FirVariable.transformInitializer(transformer: FirTransformer<D>, data: D): FirVariable  = 
    apply { replaceInitializer(initializer?.transform(transformer, data)) }

inline fun <D> FirVariable.transformDelegate(transformer: FirTransformer<D>, data: D): FirVariable  = 
    apply { replaceDelegate(delegate?.transform(transformer, data)) }

inline fun <D> FirVariable.transformGetter(transformer: FirTransformer<D>, data: D): FirVariable  = 
    apply { replaceGetter(getter?.transform(transformer, data)) }

inline fun <D> FirVariable.transformSetter(transformer: FirTransformer<D>, data: D): FirVariable  = 
    apply { replaceSetter(setter?.transform(transformer, data)) }

inline fun <D> FirVariable.transformBackingField(transformer: FirTransformer<D>, data: D): FirVariable  = 
    apply { replaceBackingField(backingField?.transform(transformer, data)) }

inline fun <D> FirVariable.transformAnnotations(transformer: FirTransformer<D>, data: D): FirVariable  = 
    apply { replaceAnnotations(annotations.transform(transformer, data)) }
