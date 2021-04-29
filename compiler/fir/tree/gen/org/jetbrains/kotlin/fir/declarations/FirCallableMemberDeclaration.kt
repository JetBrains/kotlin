/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

sealed interface FirCallableMemberDeclaration<F : FirCallableMemberDeclaration<F>> : FirCallableDeclaration<F>, FirMemberDeclaration {
    override val source: FirSourceElement?
    override val moduleData: FirModuleData
    override val resolvePhase: FirResolvePhase
    override val origin: FirDeclarationOrigin
    override val attributes: FirDeclarationAttributes
    override val annotations: List<FirAnnotationCall>
    override val returnTypeRef: FirTypeRef
    override val receiverTypeRef: FirTypeRef?
    override val symbol: FirCallableSymbol<F>
    override val typeParameters: List<FirTypeParameterRef>
    override val status: FirDeclarationStatus
    val containerSource: DeserializedContainerSource?
    val dispatchReceiverType: ConeKotlinType?

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitCallableMemberDeclaration(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E: FirElement, D> transform(transformer: FirTransformer<D>, data: D): E = 
        transformer.transformCallableMemberDeclaration(this, data) as E

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef)

    override fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?)

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirCallableMemberDeclaration<F>

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirCallableMemberDeclaration<F>

    override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirCallableMemberDeclaration<F>

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirCallableMemberDeclaration<F>

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirCallableMemberDeclaration<F>
}
