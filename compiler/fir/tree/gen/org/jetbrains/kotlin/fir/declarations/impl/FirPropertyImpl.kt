/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.impl.FirEmptyControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirPropertyImpl(
    override val source: FirSourceElement?,
    override val session: FirSession,
    override var returnTypeRef: FirTypeRef,
    override var receiverTypeRef: FirTypeRef?,
    override val name: Name,
    override var initializer: FirExpression?,
    override var delegate: FirExpression?,
    override val isVar: Boolean,
    override val symbol: FirPropertySymbol,
    override val isLocal: Boolean,
    override var status: FirDeclarationStatus
) : FirProperty(), FirModifiableVariable<FirProperty>, FirModifiableTypeParametersOwner, FirAbstractAnnotatedElement {
    override var resolvePhase: FirResolvePhase = if (isLocal) FirResolvePhase.DECLARATIONS else FirResolvePhase.RAW_FIR
    override val delegateFieldSymbol: FirDelegateFieldSymbol<FirProperty>? = delegate?.let { FirDelegateFieldSymbol(symbol.callableId) }
    override val isVal: Boolean get() = !isVar
    override var getter: FirPropertyAccessor? = null
    override var setter: FirPropertyAccessor? = null
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override var controlFlowGraphReference: FirControlFlowGraphReference = FirEmptyControlFlowGraphReference()
    override var containerSource: DeserializedContainerSource? = null
    override val backingFieldSymbol: FirBackingFieldSymbol = FirBackingFieldSymbol(symbol.callableId)
    override val typeParameters: MutableList<FirTypeParameter> = mutableListOf()

    init {
        delegateFieldSymbol?.bind(this)
        symbol.bind(this)
        backingFieldSymbol.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        returnTypeRef.accept(visitor, data)
        receiverTypeRef?.accept(visitor, data)
        initializer?.accept(visitor, data)
        delegate?.accept(visitor, data)
        getter?.accept(visitor, data)
        setter?.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        controlFlowGraphReference.accept(visitor, data)
        typeParameters.forEach { it.accept(visitor, data) }
        status.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirPropertyImpl {
        transformReturnTypeRef(transformer, data)
        transformReceiverTypeRef(transformer, data)
        transformInitializer(transformer, data)
        transformGetter(transformer, data)
        transformSetter(transformer, data)
        transformControlFlowGraphReference(transformer, data)
        transformStatus(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirPropertyImpl {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirPropertyImpl {
        receiverTypeRef = receiverTypeRef?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformInitializer(transformer: FirTransformer<D>, data: D): FirPropertyImpl {
        initializer = initializer?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformGetter(transformer: FirTransformer<D>, data: D): FirPropertyImpl {
        getter = getter?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformSetter(transformer: FirTransformer<D>, data: D): FirPropertyImpl {
        setter = setter?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirPropertyImpl {
        controlFlowGraphReference = controlFlowGraphReference.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirPropertyImpl {
        status = status.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirPropertyImpl {
        delegate = delegate?.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }
}
