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

abstract class FirValueParameter : FirVariable(), FirControlFlowGraphOwner {
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
    abstract override val name: Name
    abstract override val initializer: FirExpression?
    abstract override val delegate: FirExpression?
    abstract override val isVar: Boolean
    abstract override val isVal: Boolean
    abstract override val getter: FirPropertyAccessor?
    abstract override val setter: FirPropertyAccessor?
    abstract override val backingField: FirBackingField?
    abstract override val annotations: List<FirAnnotation>
    abstract override val controlFlowGraphReference: FirControlFlowGraphReference?
    abstract override val symbol: FirValueParameterSymbol
    abstract val defaultValue: FirExpression?
    abstract val isCrossinline: Boolean
    abstract val isNoinline: Boolean
    abstract val isVararg: Boolean


    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    abstract override fun replaceTypeParameters(newTypeParameters: List<FirTypeParameterRef>)

    abstract override fun replaceStatus(newStatus: FirDeclarationStatus)

    abstract override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef)

    abstract override fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?)

    abstract override fun replaceDeprecation(newDeprecation: DeprecationsPerUseSite?)

    abstract override fun replaceContextReceivers(newContextReceivers: List<FirContextReceiver>)

    abstract override fun replaceInitializer(newInitializer: FirExpression?)

    abstract override fun replaceDelegate(newDelegate: FirExpression?)

    abstract override fun replaceGetter(newGetter: FirPropertyAccessor?)

    abstract override fun replaceSetter(newSetter: FirPropertyAccessor?)

    abstract override fun replaceBackingField(newBackingField: FirBackingField?)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?)

    abstract fun replaceDefaultValue(newDefaultValue: FirExpression?)
}

inline fun <D> FirValueParameter.transformTypeParameters(transformer: FirTransformer<D>, data: D): FirValueParameter  = 
    apply { replaceTypeParameters(typeParameters.transform(transformer, data)) }

inline fun <D> FirValueParameter.transformStatus(transformer: FirTransformer<D>, data: D): FirValueParameter  = 
    apply { replaceStatus(status.transform(transformer, data)) }

inline fun <D> FirValueParameter.transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirValueParameter  = 
    apply { replaceReturnTypeRef(returnTypeRef.transform(transformer, data)) }

inline fun <D> FirValueParameter.transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirValueParameter  = 
    apply { replaceReceiverTypeRef(receiverTypeRef?.transform(transformer, data)) }

inline fun <D> FirValueParameter.transformContextReceivers(transformer: FirTransformer<D>, data: D): FirValueParameter  = 
    apply { replaceContextReceivers(contextReceivers.transform(transformer, data)) }

inline fun <D> FirValueParameter.transformInitializer(transformer: FirTransformer<D>, data: D): FirValueParameter  = 
    apply { replaceInitializer(initializer?.transform(transformer, data)) }

inline fun <D> FirValueParameter.transformDelegate(transformer: FirTransformer<D>, data: D): FirValueParameter  = 
    apply { replaceDelegate(delegate?.transform(transformer, data)) }

inline fun <D> FirValueParameter.transformGetter(transformer: FirTransformer<D>, data: D): FirValueParameter  = 
    apply { replaceGetter(getter?.transform(transformer, data)) }

inline fun <D> FirValueParameter.transformSetter(transformer: FirTransformer<D>, data: D): FirValueParameter  = 
    apply { replaceSetter(setter?.transform(transformer, data)) }

inline fun <D> FirValueParameter.transformBackingField(transformer: FirTransformer<D>, data: D): FirValueParameter  = 
    apply { replaceBackingField(backingField?.transform(transformer, data)) }

inline fun <D> FirValueParameter.transformAnnotations(transformer: FirTransformer<D>, data: D): FirValueParameter  = 
    apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirValueParameter.transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirValueParameter  = 
    apply { replaceControlFlowGraphReference(controlFlowGraphReference?.transform(transformer, data)) }

inline fun <D> FirValueParameter.transformDefaultValue(transformer: FirTransformer<D>, data: D): FirValueParameter  = 
    apply { replaceDefaultValue(defaultValue?.transform(transformer, data)) }
