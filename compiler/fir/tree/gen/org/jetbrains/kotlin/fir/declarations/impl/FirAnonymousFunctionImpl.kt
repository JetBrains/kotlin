/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.InlineStatus
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirAnonymousFunctionImpl(
    override val source: FirSourceElement?,
    override val moduleData: FirModuleData,
    override val origin: FirDeclarationOrigin,
    override val attributes: FirDeclarationAttributes,
    override val annotations: MutableList<FirAnnotationCall>,
    override var returnTypeRef: FirTypeRef,
    override var receiverTypeRef: FirTypeRef?,
    override var controlFlowGraphReference: FirControlFlowGraphReference?,
    override val valueParameters: MutableList<FirValueParameter>,
    override var body: FirBlock?,
    override var typeRef: FirTypeRef,
    override val symbol: FirAnonymousFunctionSymbol,
    override var label: FirLabel?,
    override var invocationKind: EventOccurrencesRange?,
    override var inlineStatus: InlineStatus,
    override val isLambda: Boolean,
    override val typeParameters: MutableList<FirTypeParameter>,
) : FirAnonymousFunction() {
    override var resolvePhase: FirResolvePhase = FirResolvePhase.DECLARATIONS

    init {
        symbol.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        returnTypeRef.accept(visitor, data)
        receiverTypeRef?.accept(visitor, data)
        controlFlowGraphReference?.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
        typeRef.accept(visitor, data)
        label?.accept(visitor, data)
        typeParameters.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirAnonymousFunctionImpl {
        transformAnnotations(transformer, data)
        transformReturnTypeRef(transformer, data)
        transformReceiverTypeRef(transformer, data)
        controlFlowGraphReference = controlFlowGraphReference?.transform(transformer, data)
        transformValueParameters(transformer, data)
        transformBody(transformer, data)
        typeRef = typeRef.transform(transformer, data)
        label = label?.transform(transformer, data)
        transformTypeParameters(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirAnonymousFunctionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirAnonymousFunctionImpl {
        returnTypeRef = returnTypeRef.transform(transformer, data)
        return this
    }

    override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirAnonymousFunctionImpl {
        receiverTypeRef = receiverTypeRef?.transform(transformer, data)
        return this
    }

    override fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirAnonymousFunctionImpl {
        valueParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformBody(transformer: FirTransformer<D>, data: D): FirAnonymousFunctionImpl {
        body = body?.transform(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirAnonymousFunctionImpl {
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }

    override fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?) {
        receiverTypeRef = newReceiverTypeRef
    }

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {
        controlFlowGraphReference = newControlFlowGraphReference
    }

    override fun replaceValueParameters(newValueParameters: List<FirValueParameter>) {
        valueParameters.clear()
        valueParameters.addAll(newValueParameters)
    }

    override fun replaceBody(newBody: FirBlock?) {
        body = newBody
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }

    override fun replaceInvocationKind(newInvocationKind: EventOccurrencesRange?) {
        invocationKind = newInvocationKind
    }

    override fun replaceInlineStatus(newInlineStatus: InlineStatus) {
        inlineStatus = newInlineStatus
    }
}
