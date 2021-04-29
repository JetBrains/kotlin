/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirErrorFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirErrorTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirErrorFunctionImpl(
    override val source: FirSourceElement?,
    override val moduleData: FirModuleData,
    override var resolvePhase: FirResolvePhase,
    override val origin: FirDeclarationOrigin,
    override val attributes: FirDeclarationAttributes,
    override val annotations: MutableList<FirAnnotationCall>,
    override val valueParameters: MutableList<FirValueParameter>,
    override val diagnostic: ConeDiagnostic,
    override val symbol: FirErrorFunctionSymbol,
    override val typeParameters: MutableList<FirTypeParameter>,
) : FirErrorFunction() {
    override var returnTypeRef: FirTypeRef = FirErrorTypeRefImpl(null, null, diagnostic)
    override val receiverTypeRef: FirTypeRef? get() = null
    override var controlFlowGraphReference: FirControlFlowGraphReference? = null
    override val body: FirBlock? get() = null

    init {
        symbol.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        returnTypeRef.accept(visitor, data)
        controlFlowGraphReference?.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirErrorFunctionImpl {
        transformAnnotations(transformer, data)
        transformReturnTypeRef(transformer, data)
        controlFlowGraphReference = controlFlowGraphReference?.transform(transformer, data)
        transformValueParameters(transformer, data)
        transformTypeParameters(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirErrorFunctionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirErrorFunctionImpl {
        returnTypeRef = returnTypeRef.transform(transformer, data)
        return this
    }

    override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirErrorFunctionImpl {
        return this
    }

    override fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirErrorFunctionImpl {
        valueParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformBody(transformer: FirTransformer<D>, data: D): FirErrorFunctionImpl {
        return this
    }

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirErrorFunctionImpl {
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }

    override fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?) {}

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {
        controlFlowGraphReference = newControlFlowGraphReference
    }

    override fun replaceValueParameters(newValueParameters: List<FirValueParameter>) {
        valueParameters.clear()
        valueParameters.addAll(newValueParameters)
    }

    override fun replaceBody(newBody: FirBlock?) {}
}
