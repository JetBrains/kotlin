/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirNamedReference
import org.jetbrains.kotlin.fir.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose

internal object MapArguments : FirTransformer<Map<FirElement, FirElement>>() {
    override fun <E : FirElement> transformElement(element: E, data: Map<FirElement, FirElement>): CompositeTransformResult<E> {
        return ((data[element] ?: element) as E).compose()
    }

    override fun transformFunctionCall(
        functionCall: FirFunctionCall,
        data: Map<FirElement, FirElement>
    ): CompositeTransformResult<FirStatement> {
        return (functionCall.transformArguments(this, data) as FirStatement).compose()
    }

    override fun transformWrappedArgumentExpression(
        wrappedArgumentExpression: FirWrappedArgumentExpression,
        data: Map<FirElement, FirElement>
    ): CompositeTransformResult<FirStatement> {
        return (wrappedArgumentExpression.transformChildren(this, data) as FirStatement).compose()
    }
}

internal object StoreType : FirTransformer<FirTypeRef>() {
    override fun <E : FirElement> transformElement(element: E, data: FirTypeRef): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformTypeRef(typeRef: FirTypeRef, data: FirTypeRef): CompositeTransformResult<FirTypeRef> {
        return data.compose()
    }
}

internal object TransformImplicitType : FirTransformer<FirTypeRef>() {
    override fun <E : FirElement> transformElement(element: E, data: FirTypeRef): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformImplicitTypeRef(
        implicitTypeRef: FirImplicitTypeRef,
        data: FirTypeRef
    ): CompositeTransformResult<FirTypeRef> {
        return data.compose()
    }
}


internal object StoreNameReference : FirTransformer<FirNamedReference>() {
    override fun <E : FirElement> transformElement(element: E, data: FirNamedReference): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformNamedReference(
        namedReference: FirNamedReference,
        data: FirNamedReference
    ): CompositeTransformResult<FirNamedReference> {
        return data.compose()
    }
}

internal object StoreCalleeReference : FirTransformer<FirResolvedCallableReference>() {
    override fun <E : FirElement> transformElement(element: E, data: FirResolvedCallableReference): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformNamedReference(
        namedReference: FirNamedReference,
        data: FirResolvedCallableReference
    ): CompositeTransformResult<FirNamedReference> {
        return data.compose()
    }

    override fun transformResolvedCallableReference(
        resolvedCallableReference: FirResolvedCallableReference,
        data: FirResolvedCallableReference
    ): CompositeTransformResult<FirNamedReference> {
        return data.compose()
    }
}

internal object StoreReceiver : FirTransformer<FirExpression>() {
    override fun <E : FirElement> transformElement(element: E, data: FirExpression): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformExpression(expression: FirExpression, data: FirExpression): CompositeTransformResult<FirStatement> {
        if (expression !is FirNoReceiverExpression) return expression.compose()
        return data.compose()
    }
}