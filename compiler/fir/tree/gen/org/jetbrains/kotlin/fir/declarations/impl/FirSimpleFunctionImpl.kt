/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.impl.FirEmptyContractDescription
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.impl.FirEmptyControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

open class FirSimpleFunctionImpl(
    override val source: FirSourceElement?,
    override val session: FirSession,
    override var returnTypeRef: FirTypeRef,
    override var receiverTypeRef: FirTypeRef?,
    override val name: Name,
    override var status: FirDeclarationStatus,
    override val symbol: FirFunctionSymbol<FirSimpleFunction>
) : FirSimpleFunction(), FirModifiableFunction<FirSimpleFunction>, FirModifiableTypeParametersOwner, FirAbstractAnnotatedElement {
    override var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override var controlFlowGraphReference: FirControlFlowGraphReference = FirEmptyControlFlowGraphReference()
    override val typeParameters: MutableList<FirTypeParameter> = mutableListOf()
    override val valueParameters: MutableList<FirValueParameter> = mutableListOf()
    override var body: FirBlock? = null
    override var containerSource: DeserializedContainerSource? = null
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override var contractDescription: FirContractDescription = FirEmptyContractDescription

    init {
        symbol.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        returnTypeRef.accept(visitor, data)
        receiverTypeRef?.accept(visitor, data)
        controlFlowGraphReference.accept(visitor, data)
        typeParameters.forEach { it.accept(visitor, data) }
        valueParameters.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
        status.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        contractDescription.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirSimpleFunctionImpl {
        transformReturnTypeRef(transformer, data)
        transformReceiverTypeRef(transformer, data)
        transformControlFlowGraphReference(transformer, data)
        typeParameters.transformInplace(transformer, data)
        transformValueParameters(transformer, data)
        body = body?.transformSingle(transformer, data)
        transformStatus(transformer, data)
        annotations.transformInplace(transformer, data)
        transformContractDescription(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirSimpleFunctionImpl {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirSimpleFunctionImpl {
        receiverTypeRef = receiverTypeRef?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirSimpleFunctionImpl {
        controlFlowGraphReference = controlFlowGraphReference.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirSimpleFunctionImpl {
        valueParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirSimpleFunctionImpl {
        status = status.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformContractDescription(transformer: FirTransformer<D>, data: D): FirSimpleFunctionImpl {
        contractDescription = contractDescription.transformSingle(transformer, data)
        return this
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }
}
