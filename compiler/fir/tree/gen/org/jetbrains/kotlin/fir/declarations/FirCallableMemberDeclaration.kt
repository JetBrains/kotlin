/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirCallableMemberDeclaration<F : FirCallableMemberDeclaration<F>> : FirCallableDeclaration<F>, FirMemberDeclaration {
    override val source: FirSourceElement?
    override val session: FirSession
    override val resolvePhase: FirResolvePhase
    override val annotations: List<FirAnnotationCall>
    override val returnTypeRef: FirTypeRef
    override val receiverTypeRef: FirTypeRef?
    override val symbol: FirCallableSymbol<F>
    override val name: Name
    override val typeParameters: List<FirTypeParameter>
    override val status: FirDeclarationStatus
    val containerSource: DeserializedContainerSource?

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitCallableMemberDeclaration(this, data)

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirCallableMemberDeclaration<F>

    override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirCallableMemberDeclaration<F>

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirCallableMemberDeclaration<F>
}
