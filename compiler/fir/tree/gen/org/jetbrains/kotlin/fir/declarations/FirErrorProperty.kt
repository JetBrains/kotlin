/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorPropertySymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirErrorProperty : FirVariable<FirErrorProperty>(), FirDiagnosticHolder, FirTypeParametersOwner {
    abstract override val source: FirSourceElement?
    abstract override val session: FirSession
    abstract override val resolvePhase: FirResolvePhase
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val returnTypeRef: FirTypeRef
    abstract override val receiverTypeRef: FirTypeRef?
    abstract override val name: Name
    abstract override val initializer: FirExpression?
    abstract override val delegate: FirExpression?
    abstract override val delegateFieldSymbol: FirDelegateFieldSymbol<FirErrorProperty>?
    abstract override val isVar: Boolean
    abstract override val isVal: Boolean
    abstract override val getter: FirPropertyAccessor?
    abstract override val setter: FirPropertyAccessor?
    abstract override val annotations: List<FirAnnotationCall>
    abstract override val diagnostic: ConeDiagnostic
    abstract override val typeParameters: List<FirTypeParameter>
    abstract override val symbol: FirErrorPropertySymbol

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitErrorProperty(this, data)

    abstract override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    abstract override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef)

    abstract override fun replaceReceiverTypeRef(newReceiverTypeRef: FirTypeRef?)

    abstract override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirErrorProperty

    abstract override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirErrorProperty

    abstract override fun <D> transformInitializer(transformer: FirTransformer<D>, data: D): FirErrorProperty

    abstract override fun <D> transformDelegate(transformer: FirTransformer<D>, data: D): FirErrorProperty

    abstract override fun <D> transformGetter(transformer: FirTransformer<D>, data: D): FirErrorProperty

    abstract override fun <D> transformSetter(transformer: FirTransformer<D>, data: D): FirErrorProperty

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirErrorProperty

    abstract override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirErrorProperty

    abstract override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirErrorProperty
}
