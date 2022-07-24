/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")
package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirErrorProperty : FirVariable(), FirDiagnosticHolder {
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
    abstract override val diagnostic: ConeDiagnostic
    abstract override val symbol: FirErrorPropertySymbol


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
}

inline fun <D> FirErrorProperty.transformTypeParameters(transformer: FirTransformer<D>, data: D): FirErrorProperty  = 
    apply { replaceTypeParameters(typeParameters.transform(transformer, data)) }

inline fun <D> FirErrorProperty.transformStatus(transformer: FirTransformer<D>, data: D): FirErrorProperty  = 
    apply { replaceStatus(status.transform(transformer, data)) }

inline fun <D> FirErrorProperty.transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirErrorProperty  = 
    apply { replaceReturnTypeRef(returnTypeRef.transform(transformer, data)) }

inline fun <D> FirErrorProperty.transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirErrorProperty  = 
    apply { replaceReceiverTypeRef(receiverTypeRef?.transform(transformer, data)) }

inline fun <D> FirErrorProperty.transformContextReceivers(transformer: FirTransformer<D>, data: D): FirErrorProperty  = 
    apply { replaceContextReceivers(contextReceivers.transform(transformer, data)) }

inline fun <D> FirErrorProperty.transformInitializer(transformer: FirTransformer<D>, data: D): FirErrorProperty  = 
    apply { replaceInitializer(initializer?.transform(transformer, data)) }

inline fun <D> FirErrorProperty.transformDelegate(transformer: FirTransformer<D>, data: D): FirErrorProperty  = 
    apply { replaceDelegate(delegate?.transform(transformer, data)) }

inline fun <D> FirErrorProperty.transformGetter(transformer: FirTransformer<D>, data: D): FirErrorProperty  = 
    apply { replaceGetter(getter?.transform(transformer, data)) }

inline fun <D> FirErrorProperty.transformSetter(transformer: FirTransformer<D>, data: D): FirErrorProperty  = 
    apply { replaceSetter(setter?.transform(transformer, data)) }

inline fun <D> FirErrorProperty.transformBackingField(transformer: FirTransformer<D>, data: D): FirErrorProperty  = 
    apply { replaceBackingField(backingField?.transform(transformer, data)) }

inline fun <D> FirErrorProperty.transformAnnotations(transformer: FirTransformer<D>, data: D): FirErrorProperty  = 
    apply { replaceAnnotations(annotations.transform(transformer, data)) }
