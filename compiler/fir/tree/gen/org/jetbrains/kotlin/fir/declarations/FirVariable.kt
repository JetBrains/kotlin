/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirVariable<F : FirVariable<F>> : FirCallableDeclaration<F>, FirNamedDeclaration, FirStatement {
    override val source: FirSourceElement?
    override val session: FirSession
    override val resolvePhase: FirResolvePhase
    override val returnTypeRef: FirTypeRef
    override val receiverTypeRef: FirTypeRef?
    override val name: Name
    override val symbol: FirVariableSymbol<F>
    val initializer: FirExpression?
    val delegate: FirExpression?
    val delegateFieldSymbol: FirDelegateFieldSymbol<F>?
    val isVar: Boolean
    val isVal: Boolean
    val getter: FirPropertyAccessor?
    val setter: FirPropertyAccessor?
    override val annotations: List<FirAnnotationCall>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitVariable(this, data)

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirVariable<F>

    override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirVariable<F>

    fun <D> transformInitializer(transformer: FirTransformer<D>, data: D): FirVariable<F>

    fun <D> transformGetter(transformer: FirTransformer<D>, data: D): FirVariable<F>

    fun <D> transformSetter(transformer: FirTransformer<D>, data: D): FirVariable<F>

    fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirVariable<F>
}
