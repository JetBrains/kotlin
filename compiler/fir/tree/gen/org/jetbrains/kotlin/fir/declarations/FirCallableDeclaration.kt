/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

sealed class FirCallableDeclaration : FirTypedDeclaration() {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract override val moduleData: FirModuleData
    abstract override val resolvePhase: FirResolvePhase
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val typeParameters: List<FirTypeParameterRef>
    abstract override val status: FirDeclarationStatus
    abstract override val returnTypeRef: FirTypeRef
    abstract val receiverTypeRef: FirTypeRef?
    abstract val deprecation: DeprecationsPerUseSite?
    abstract override val symbol: FirCallableSymbol<out FirCallableDeclaration>
    abstract val containerSource: DeserializedContainerSource?
    abstract val dispatchReceiverType: ConeKotlinType?

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitCallableDeclaration(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E: FirElement, D> transform(transformer: FirTransformer<D>, data: D): E = 
        transformer.transformCallableDeclaration(this, data) as E

    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    abstract override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef)

    abstract fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?)

    abstract fun replaceDeprecation(newDeprecation: DeprecationsPerUseSite?)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirCallableDeclaration

    abstract override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirCallableDeclaration

    abstract override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirCallableDeclaration

    abstract override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirCallableDeclaration

    abstract fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirCallableDeclaration
}
