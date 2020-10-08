/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

open class FirValueParameterImpl @FirImplementationDetail constructor(
    override val source: FirSourceElement?,
    override val session: FirSession,
    override var resolvePhase: FirResolvePhase,
    override val origin: FirDeclarationOrigin,
    override var returnTypeRef: FirTypeRef,
    override val name: Name,
    override val symbol: FirVariableSymbol<FirValueParameter>,
    override val annotations: MutableList<FirAnnotationCall>,
    override var defaultValue: FirExpression?,
    override val isCrossinline: Boolean,
    override val isNoinline: Boolean,
    override val isVararg: Boolean,
) : FirValueParameter() {
    override val attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    override val receiverTypeRef: FirTypeRef? get() = null
    override val initializer: FirExpression? get() = null
    override val delegate: FirExpression? get() = null
    override val delegateFieldSymbol: FirDelegateFieldSymbol<FirValueParameter>? get() = null
    override val isVar: Boolean get() = false
    override val isVal: Boolean get() = true
    override val getter: FirPropertyAccessor? get() = null
    override val setter: FirPropertyAccessor? get() = null
    override var controlFlowGraphReference: FirControlFlowGraphReference? = null

    init {
        symbol.bind(this)
        delegateFieldSymbol?.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        returnTypeRef.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        controlFlowGraphReference?.accept(visitor, data)
        defaultValue?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirValueParameterImpl {
        transformReturnTypeRef(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirValueParameterImpl {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirValueParameterImpl {
        return this
    }

    override fun <D> transformInitializer(transformer: FirTransformer<D>, data: D): FirValueParameterImpl {
        return this
    }

    override fun <D> transformDelegate(transformer: FirTransformer<D>, data: D): FirValueParameterImpl {
        return this
    }

    override fun <D> transformGetter(transformer: FirTransformer<D>, data: D): FirValueParameterImpl {
        return this
    }

    override fun <D> transformSetter(transformer: FirTransformer<D>, data: D): FirValueParameterImpl {
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirValueParameterImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirValueParameterImpl {
        transformAnnotations(transformer, data)
        controlFlowGraphReference = controlFlowGraphReference?.transformSingle(transformer, data)
        defaultValue = defaultValue?.transformSingle(transformer, data)
        return this
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }

    override fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?) {}

    override fun replaceInitializer(newInitializer: FirExpression?) {}

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?) {
        controlFlowGraphReference = newControlFlowGraphReference
    }
}
