/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.contracts.description.InvocationKind
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.impl.FirEmptyControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirAnonymousFunctionImpl(
    override val source: FirSourceElement?,
    override val session: FirSession,
    override var returnTypeRef: FirTypeRef,
    override var receiverTypeRef: FirTypeRef?,
    override val symbol: FirAnonymousFunctionSymbol,
    override val isLambda: Boolean
) : FirAnonymousFunction(), FirModifiableFunction<FirAnonymousFunction>, FirAbstractAnnotatedElement {
    override var resolvePhase: FirResolvePhase = FirResolvePhase.DECLARATIONS
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override var controlFlowGraphReference: FirControlFlowGraphReference = FirEmptyControlFlowGraphReference()
    override val typeParameters: MutableList<FirTypeParameter> = mutableListOf()
    override val valueParameters: MutableList<FirValueParameter> = mutableListOf()
    override var body: FirBlock? = null
    override var typeRef: FirTypeRef = FirImplicitTypeRefImpl(null)
    override var label: FirLabel? = null
    override var invocationKind: InvocationKind? = null

    init {
        symbol.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        returnTypeRef.accept(visitor, data)
        receiverTypeRef?.accept(visitor, data)
        controlFlowGraphReference.accept(visitor, data)
        typeParameters.forEach { it.accept(visitor, data) }
        valueParameters.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
        typeRef.accept(visitor, data)
        label?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirAnonymousFunctionImpl {
        annotations.transformInplace(transformer, data)
        transformReturnTypeRef(transformer, data)
        transformReceiverTypeRef(transformer, data)
        transformControlFlowGraphReference(transformer, data)
        typeParameters.transformInplace(transformer, data)
        transformValueParameters(transformer, data)
        body = body?.transformSingle(transformer, data)
        typeRef = typeRef.transformSingle(transformer, data)
        label = label?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirAnonymousFunctionImpl {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirAnonymousFunctionImpl {
        receiverTypeRef = receiverTypeRef?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirAnonymousFunctionImpl {
        controlFlowGraphReference = controlFlowGraphReference.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirAnonymousFunctionImpl {
        valueParameters.transformInplace(transformer, data)
        return this
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }

    override fun replaceInvocationKind(newInvocationKind: InvocationKind) {
        invocationKind = newInvocationKind
    }
}
