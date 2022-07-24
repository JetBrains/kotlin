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
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

sealed class FirCallableDeclaration : FirMemberDeclaration() {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract override val moduleData: FirModuleData
    abstract override val resolvePhase: FirResolvePhase
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val typeParameters: List<FirTypeParameterRef>
    abstract override val status: FirDeclarationStatus
    abstract val returnTypeRef: FirTypeRef
    abstract val receiverTypeRef: FirTypeRef?
    abstract val deprecation: DeprecationsPerUseSite?
    abstract override val symbol: FirCallableSymbol<out FirCallableDeclaration>
    abstract val containerSource: DeserializedContainerSource?
    abstract val dispatchReceiverType: ConeSimpleKotlinType?
    abstract val contextReceivers: List<FirContextReceiver>


    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    abstract override fun replaceTypeParameters(newTypeParameters: List<FirTypeParameterRef>)

    abstract override fun replaceStatus(newStatus: FirDeclarationStatus)

    abstract fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef)

    abstract fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?)

    abstract fun replaceDeprecation(newDeprecation: DeprecationsPerUseSite?)

    abstract fun replaceContextReceivers(newContextReceivers: List<FirContextReceiver>)
}

inline fun <D> FirCallableDeclaration.transformAnnotations(transformer: FirTransformer<D>, data: D): FirCallableDeclaration  = 
    apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirCallableDeclaration.transformTypeParameters(transformer: FirTransformer<D>, data: D): FirCallableDeclaration  = 
    apply { replaceTypeParameters(typeParameters.transform(transformer, data)) }

inline fun <D> FirCallableDeclaration.transformStatus(transformer: FirTransformer<D>, data: D): FirCallableDeclaration  = 
    apply { replaceStatus(status.transform(transformer, data)) }

inline fun <D> FirCallableDeclaration.transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirCallableDeclaration  = 
    apply { replaceReturnTypeRef(returnTypeRef.transform(transformer, data)) }

inline fun <D> FirCallableDeclaration.transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirCallableDeclaration  = 
    apply { replaceReceiverTypeRef(receiverTypeRef?.transform(transformer, data)) }

inline fun <D> FirCallableDeclaration.transformContextReceivers(transformer: FirTransformer<D>, data: D): FirCallableDeclaration  = 
    apply { replaceContextReceivers(contextReceivers.transform(transformer, data)) }
