/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirVariable
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

interface FirModifiableVariable<F : FirVariable<F>>  : FirVariable<F>, FirAbstractAnnotatedElement {
    override val source: FirSourceElement?
    override val session: FirSession
    override var resolvePhase: FirResolvePhase
    override var returnTypeRef: FirTypeRef
    override var receiverTypeRef: FirTypeRef?
    override val name: Name
    override val symbol: FirVariableSymbol<F>
    override var initializer: FirExpression?
    override var delegate: FirExpression?
    override val delegateFieldSymbol: FirDelegateFieldSymbol<F>?
    override val isVar: Boolean
    override val isVal: Boolean
    override var getter: FirPropertyAccessor?
    override var setter: FirPropertyAccessor?
    override val annotations: MutableList<FirAnnotationCall>
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirModifiableVariable<F>

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirModifiableVariable<F>

    override fun <D> transformReceiverTypeRef(transformer: FirTransformer<D>, data: D): FirModifiableVariable<F>

    override fun <D> transformInitializer(transformer: FirTransformer<D>, data: D): FirModifiableVariable<F>

    override fun <D> transformGetter(transformer: FirTransformer<D>, data: D): FirModifiableVariable<F>

    override fun <D> transformSetter(transformer: FirTransformer<D>, data: D): FirModifiableVariable<F>

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirModifiableVariable<F>

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef)
}
