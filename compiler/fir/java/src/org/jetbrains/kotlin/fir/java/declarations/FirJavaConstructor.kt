/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.CONSTRUCTOR_NAME
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.impl.FirEmptyControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class FirJavaConstructor(
    override val source: FirSourceElement?,
    override val session: FirSession,
    override val symbol: FirConstructorSymbol,
    visibility: Visibility,
    override val isPrimary: Boolean,
    isInner: Boolean,
    override var returnTypeRef : FirTypeRef
) : FirPureAbstractElement(), FirAbstractAnnotatedElement, FirConstructor {
    override val receiverTypeRef: FirTypeRef? get() = null
    override val typeParameters: MutableList<FirTypeParameter> = mutableListOf()
    override val name: Name get() = CONSTRUCTOR_NAME
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()

    override var status: FirDeclarationStatus = FirDeclarationStatusImpl(visibility, Modality.FINAL).apply {
        isExpect = false
        isActual = false
        isOverride = false
        this.isInner = isInner
    }

    override var resolvePhase: FirResolvePhase = FirResolvePhase.DECLARATIONS

    init {
        symbol.bind(this)
    }

    override val delegatedConstructor: FirDelegatedConstructorCall?
        get() = null

    override val body: FirBlock?
        get() = null

    override val valueParameters = mutableListOf<FirValueParameter>()

    override val controlFlowGraphReference: FirControlFlowGraphReference get() = FirEmptyControlFlowGraphReference()

    override fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirJavaConstructor {
        valueParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirJavaConstructor {
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirConstructor {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        returnTypeRef.accept(visitor, data)
        controlFlowGraphReference.accept(visitor, data)
        typeParameters.forEach { it.accept(visitor, data) }
        valueParameters.forEach { it.accept(visitor, data) }
        status.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirJavaConstructor {
        transformReturnTypeRef(transformer, data)
        transformControlFlowGraphReference(transformer, data)
        typeParameters.transformInplace(transformer, data)
        transformValueParameters(transformer, data)
        status = status.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirJavaConstructor {
        return this
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirJavaConstructor {
        status = status.transformSingle(transformer, data)
        return this
    }

    override var containerSource: DeserializedContainerSource? = null
}