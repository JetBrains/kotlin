/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

/**
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.FirTree.callableDeclaration]
 */
sealed class FirCallableDeclaration : FirMemberDeclaration() {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract override val moduleData: FirModuleData
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val typeParameters: List<FirTypeParameterRef>
    abstract override val status: FirDeclarationStatus
    abstract val returnTypeRef: FirTypeRef
    abstract val receiverParameter: FirReceiverParameter?
    abstract val staticReceiverParameter: FirTypeRef?
    abstract val deprecationsProvider: DeprecationsProvider
    abstract override val symbol: FirCallableSymbol<FirCallableDeclaration>
    abstract val containerSource: DeserializedContainerSource?
    abstract val dispatchReceiverType: ConeSimpleKotlinType?
    abstract val contextParameters: List<FirValueParameter>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitCallableDeclaration(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformCallableDeclaration(this, data) as E

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceStatus(newStatus: FirDeclarationStatus)

    abstract fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef)

    abstract fun replaceReceiverParameter(newReceiverParameter: FirReceiverParameter?)

    abstract fun replaceStaticReceiverParameter(newStaticReceiverParameter: FirTypeRef?)

    abstract fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider)

    abstract fun replaceContextParameters(newContextParameters: List<FirValueParameter>)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirCallableDeclaration

    abstract override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirCallableDeclaration

    abstract override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirCallableDeclaration

    abstract fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirCallableDeclaration

    abstract fun <D> transformReceiverParameter(transformer: FirTransformer<D>, data: D): FirCallableDeclaration

    abstract fun <D> transformStaticReceiverParameter(transformer: FirTransformer<D>, data: D): FirCallableDeclaration

    abstract fun <D> transformContextParameters(transformer: FirTransformer<D>, data: D): FirCallableDeclaration
}
