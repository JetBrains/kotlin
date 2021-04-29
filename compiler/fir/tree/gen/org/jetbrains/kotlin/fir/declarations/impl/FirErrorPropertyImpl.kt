/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirErrorProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorPropertySymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirErrorTypeRefImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirErrorPropertyImpl(
    override val source: FirSourceElement?,
    override val moduleData: FirModuleData,
    override var resolvePhase: FirResolvePhase,
    override val origin: FirDeclarationOrigin,
    override val attributes: FirDeclarationAttributes,
    override val name: Name,
    override val annotations: MutableList<FirAnnotationCall>,
    override val diagnostic: ConeDiagnostic,
    override val typeParameters: MutableList<FirTypeParameter>,
    override val symbol: FirErrorPropertySymbol,
) : FirErrorProperty() {
    override var returnTypeRef: FirTypeRef = FirErrorTypeRefImpl(null, null, diagnostic)
    override val receiverTypeRef: FirTypeRef? get() = null
    override val initializer: FirExpression? get() = null
    override val delegate: FirExpression? get() = null
    override val delegateFieldSymbol: FirDelegateFieldSymbol<FirErrorProperty>? get() = null
    override val isVar: Boolean get() = false
    override val isVal: Boolean get() = true
    override val getter: FirPropertyAccessor? get() = null
    override val setter: FirPropertyAccessor? get() = null

    init {
        delegateFieldSymbol?.bind(this)
        symbol.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        returnTypeRef.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirErrorPropertyImpl {
        transformReturnTypeRef(transformer, data)
        transformTypeParameters(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirErrorPropertyImpl {
        returnTypeRef = returnTypeRef.transform(transformer, data)
        return this
    }

    override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirErrorPropertyImpl {
        return this
    }

    override fun <D> transformInitializer(transformer: FirTransformer<D>, data: D): FirErrorPropertyImpl {
        return this
    }

    override fun <D> transformDelegate(transformer: FirTransformer<D>, data: D): FirErrorPropertyImpl {
        return this
    }

    override fun <D> transformGetter(transformer: FirTransformer<D>, data: D): FirErrorPropertyImpl {
        return this
    }

    override fun <D> transformSetter(transformer: FirTransformer<D>, data: D): FirErrorPropertyImpl {
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirErrorPropertyImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirErrorPropertyImpl {
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirErrorPropertyImpl {
        transformAnnotations(transformer, data)
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
}
