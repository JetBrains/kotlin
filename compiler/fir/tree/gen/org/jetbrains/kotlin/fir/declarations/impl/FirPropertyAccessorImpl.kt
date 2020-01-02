/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.impl.FirEmptyContractDescription
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.impl.FirEmptyControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

open class FirPropertyAccessorImpl(
    override val source: FirSourceElement?,
    override val session: FirSession,
    override var returnTypeRef: FirTypeRef,
    override val symbol: FirPropertyAccessorSymbol,
    override val isGetter: Boolean,
    override var status: FirDeclarationStatus
) : FirPropertyAccessor(), FirModifiableFunction<FirPropertyAccessor>, FirAbstractAnnotatedElement {
    override var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override var receiverTypeRef: FirTypeRef? = null
    override var controlFlowGraphReference: FirControlFlowGraphReference = FirEmptyControlFlowGraphReference()
    override val typeParameters: MutableList<FirTypeParameter> = mutableListOf()
    override val valueParameters: MutableList<FirValueParameter> = mutableListOf()
    override var body: FirBlock? = null
    override var contractDescription: FirContractDescription = FirEmptyContractDescription
    override val isSetter: Boolean get() = !isGetter
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()

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
        contractDescription.accept(visitor, data)
        status.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirPropertyAccessorImpl {
        transformReturnTypeRef(transformer, data)
        transformReceiverTypeRef(transformer, data)
        transformControlFlowGraphReference(transformer, data)
        typeParameters.transformInplace(transformer, data)
        transformValueParameters(transformer, data)
        body = body?.transformSingle(transformer, data)
        transformContractDescription(transformer, data)
        transformStatus(transformer, data)
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirPropertyAccessorImpl {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirPropertyAccessorImpl {
        receiverTypeRef = receiverTypeRef?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirPropertyAccessorImpl {
        controlFlowGraphReference = controlFlowGraphReference.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirPropertyAccessorImpl {
        valueParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformContractDescription(transformer: FirTransformer<D>, data: D): FirPropertyAccessorImpl {
        contractDescription = contractDescription.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirPropertyAccessorImpl {
        status = status.transformSingle(transformer, data)
        return this
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }
}
