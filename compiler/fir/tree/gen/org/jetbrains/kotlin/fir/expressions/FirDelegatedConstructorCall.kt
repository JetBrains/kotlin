/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirDelegatedConstructorCall : FirPureAbstractElement(), FirResolvable, FirCall {
    abstract override val source: FirSourceElement?
    abstract override val annotations: List<FirAnnotationCall>
    abstract override val argumentList: FirArgumentList
    abstract val constructedTypeRef: FirTypeRef
    abstract val dispatchReceiver: FirExpression
    abstract override val calleeReference: FirReference
    abstract val isThis: Boolean
    abstract val isSuper: Boolean

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitDelegatedConstructorCall(this, data)

    abstract override fun replaceArgumentList(newArgumentList: FirArgumentList)

    abstract fun replaceConstructedTypeRef(newConstructedTypeRef: FirTypeRef)

    abstract override fun replaceCalleeReference(newCalleeReference: FirReference)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirDelegatedConstructorCall

    abstract fun <D> transformDispatchReceiver(transformer: FirTransformer<D>, data: D): FirDelegatedConstructorCall

    abstract override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirDelegatedConstructorCall
}
