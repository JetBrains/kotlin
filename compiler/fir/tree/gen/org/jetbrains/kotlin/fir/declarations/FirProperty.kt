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

abstract class FirProperty : FirVariable(), FirTypeParameterRefsOwner, FirControlFlowGraphOwner {
    abstract override val source: KtSourceElement?
    abstract override val moduleData: FirModuleData
    abstract override val resolvePhase: FirResolvePhase
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
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
    abstract override val symbol: FirPropertySymbol
    abstract val delegateFieldSymbol: FirDelegateFieldSymbol?
    abstract val isLocal: Boolean
    abstract val bodyResolveState: FirPropertyBodyResolveState
    abstract override val typeParameters: List<FirTypeParameterRef>


    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

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

    abstract fun replaceBodyResolveState(newBodyResolveState: FirPropertyBodyResolveState)

    abstract override fun replaceTypeParameters(newTypeParameters: List<FirTypeParameterRef>)
}

inline fun <D> FirProperty.transformStatus(transformer: FirTransformer<D>, data: D): FirProperty  = 
    apply { replaceStatus(status.transform(transformer, data)) }

inline fun <D> FirProperty.transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirProperty  = 
    apply { replaceReturnTypeRef(returnTypeRef.transform(transformer, data)) }

inline fun <D> FirProperty.transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirProperty  = 
    apply { replaceReceiverTypeRef(receiverTypeRef?.transform(transformer, data)) }

inline fun <D> FirProperty.transformContextReceivers(transformer: FirTransformer<D>, data: D): FirProperty  = 
    apply { replaceContextReceivers(contextReceivers.transform(transformer, data)) }

inline fun <D> FirProperty.transformInitializer(transformer: FirTransformer<D>, data: D): FirProperty  = 
    apply { replaceInitializer(initializer?.transform(transformer, data)) }

inline fun <D> FirProperty.transformDelegate(transformer: FirTransformer<D>, data: D): FirProperty  = 
    apply { replaceDelegate(delegate?.transform(transformer, data)) }

inline fun <D> FirProperty.transformGetter(transformer: FirTransformer<D>, data: D): FirProperty  = 
    apply { replaceGetter(getter?.transform(transformer, data)) }

inline fun <D> FirProperty.transformSetter(transformer: FirTransformer<D>, data: D): FirProperty  = 
    apply { replaceSetter(setter?.transform(transformer, data)) }

inline fun <D> FirProperty.transformBackingField(transformer: FirTransformer<D>, data: D): FirProperty  = 
    apply { replaceBackingField(backingField?.transform(transformer, data)) }

inline fun <D> FirProperty.transformAnnotations(transformer: FirTransformer<D>, data: D): FirProperty  = 
    apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirProperty.transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirProperty  = 
    apply { replaceControlFlowGraphReference(controlFlowGraphReference?.transform(transformer, data)) }

inline fun <D> FirProperty.transformTypeParameters(transformer: FirTransformer<D>, data: D): FirProperty  = 
    apply { replaceTypeParameters(typeParameters.transform(transformer, data)) }
