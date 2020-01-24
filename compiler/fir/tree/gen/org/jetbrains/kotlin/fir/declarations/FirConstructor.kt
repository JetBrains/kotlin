/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirConstructor : FirMemberFunction<FirConstructor> {
    override val source: FirSourceElement?
    override val session: FirSession
    override val resolvePhase: FirResolvePhase
    override val returnTypeRef: FirTypeRef
    override val receiverTypeRef: FirTypeRef?
    override val controlFlowGraphReference: FirControlFlowGraphReference
    override val typeParameters: List<FirTypeParameter>
    override val valueParameters: List<FirValueParameter>
    override val name: Name
    override val status: FirDeclarationStatus
    override val containerSource: DeserializedContainerSource?
    override val annotations: List<FirAnnotationCall>
    override val symbol: FirConstructorSymbol
    val delegatedConstructor: FirDelegatedConstructorCall?
    override val body: FirBlock?
    val isPrimary: Boolean

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitConstructor(this, data)

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirConstructor

    override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirConstructor

    override fun <D> transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirConstructor

    override fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirConstructor

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirConstructor
}
