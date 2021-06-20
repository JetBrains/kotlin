/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.FirTargetElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

sealed class FirFunction<E : FirFunction<E>> : FirCallableMemberDeclaration<E>(), FirTargetElement, FirControlFlowGraphOwner, FirStatement {
    abstract override val source: FirSourceElement?
    abstract override val moduleData: FirModuleData
    abstract override val resolvePhase: FirResolvePhase
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val annotations: List<FirAnnotationCall>
    abstract override val returnTypeRef: FirTypeRef
    abstract override val receiverTypeRef: FirTypeRef?
    abstract override val typeParameters: List<FirTypeParameterRef>
    abstract override val status: FirDeclarationStatus
    abstract override val containerSource: DeserializedContainerSource?
    abstract override val dispatchReceiverType: ConeKotlinType?
    abstract override val controlFlowGraphReference: FirControlFlowGraphReference?
    abstract override val symbol: FirFunctionSymbol<E>
    abstract val valueParameters: List<FirValueParameter>
    abstract val body: FirBlock?

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitFunction(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E: FirElement, D> transform(transformer: FirTransformer<D>, data: D): E = 
        transformer.transformFunction(this, data) as E

    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    abstract override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef)

    abstract override fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?)

    abstract override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?)

    abstract fun replaceValueParameters(newValueParameters: List<FirValueParameter>)

    abstract fun replaceBody(newBody: FirBlock?)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirFunction<E>

    abstract override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirFunction<E>

    abstract override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirFunction<E>

    abstract override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirFunction<E>

    abstract override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirFunction<E>

    abstract fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirFunction<E>

    abstract fun <D> transformBody(transformer: FirTransformer<D>, data: D): FirFunction<E>
}
