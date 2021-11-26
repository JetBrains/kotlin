/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.DeprecationsPerUseSite
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirErrorFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirErrorTypeRefImpl
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirErrorFunctionImpl(
    override val source: KtSourceElement?,
    override val annotations: MutableList<FirAnnotation>,
    override val moduleData: FirModuleData,
    @Volatile
    override var resolvePhase: FirResolvePhase,
    override val origin: FirDeclarationOrigin,
    override val attributes: FirDeclarationAttributes,
    override var deprecation: DeprecationsPerUseSite?,
    override val containerSource: DeserializedContainerSource?,
    override val dispatchReceiverType: ConeKotlinType?,
    override val valueParameters: MutableList<FirValueParameter>,
    override val diagnostic: ConeDiagnostic,
    override val symbol: FirErrorFunctionSymbol,
) : FirErrorFunction() {
    override var status: FirDeclarationStatus = FirResolvedDeclarationStatusImpl.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
    override var returnTypeRef: FirTypeRef = FirErrorTypeRefImpl(null, null, diagnostic)
    override val receiverTypeRef: FirTypeRef? get() = null
    override var controlFlowGraphReference: FirControlFlowGraphReference? = null
    override val body: FirBlock? get() = null
    override val typeParameters: List<FirTypeParameter> get() = emptyList()

    init {
        symbol.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        status.accept(visitor, data)
        returnTypeRef.accept(visitor, data)
        controlFlowGraphReference?.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirErrorFunctionImpl {
        transformAnnotations(transformer, data)
        transformStatus(transformer, data)
        transformReturnTypeRef(transformer, data)
        controlFlowGraphReference = controlFlowGraphReference?.transform(transformer, data)
        transformValueParameters(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirErrorFunctionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirErrorFunctionImpl {
        status = status.transform(transformer, data)
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
        return this
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }

    override fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?) {}

    override fun replaceDeprecation(newDeprecation: DeprecationsPerUseSite?) {
        deprecation = newDeprecation
    }

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {
        controlFlowGraphReference = newControlFlowGraphReference
    }

    override fun replaceValueParameters(newValueParameters: List<FirValueParameter>) {
        valueParameters.clear()
        valueParameters.addAll(newValueParameters)
    }

    override fun replaceBody(newBody: FirBlock?) {}
}
