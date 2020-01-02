/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirDefaultSetterValueParameter(
    override val source: FirSourceElement?,
    override val session: FirSession,
    override var returnTypeRef: FirTypeRef,
    override val symbol: FirVariableSymbol<FirValueParameter>
) : FirValueParameter(), FirAbstractAnnotatedElement {
    override var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override var receiverTypeRef: FirTypeRef? = null
    override val name: Name = Name.identifier("value")
    override var initializer: FirExpression? = null
    override var delegate: FirExpression? = null
    override val delegateFieldSymbol: FirDelegateFieldSymbol<FirValueParameter>? = null
    override val isVar: Boolean = false
    override val isVal: Boolean = true
    override var getter: FirPropertyAccessor? = null
    override var setter: FirPropertyAccessor? = null
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override var defaultValue: FirExpression? = null
    override val isCrossinline: Boolean = false
    override val isNoinline: Boolean = false
    override val isVararg: Boolean = false

    init {
        symbol.bind(this)
        delegateFieldSymbol?.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        returnTypeRef.accept(visitor, data)
        receiverTypeRef?.accept(visitor, data)
        initializer?.accept(visitor, data)
        delegate?.accept(visitor, data)
        getter?.accept(visitor, data)
        setter?.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        defaultValue?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirDefaultSetterValueParameter {
        transformReturnTypeRef(transformer, data)
        transformReceiverTypeRef(transformer, data)
        transformInitializer(transformer, data)
        transformGetter(transformer, data)
        transformSetter(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirDefaultSetterValueParameter {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirDefaultSetterValueParameter {
        receiverTypeRef = receiverTypeRef?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformInitializer(transformer: FirTransformer<D>, data: D): FirDefaultSetterValueParameter {
        initializer = initializer?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformGetter(transformer: FirTransformer<D>, data: D): FirDefaultSetterValueParameter {
        getter = getter?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformSetter(transformer: FirTransformer<D>, data: D): FirDefaultSetterValueParameter {
        setter = setter?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirDefaultSetterValueParameter {
        delegate = delegate?.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
        defaultValue = defaultValue?.transformSingle(transformer, data)
        return this
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef) {
        returnTypeRef = newReturnTypeRef
    }
}
