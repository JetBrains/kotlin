/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.contracts.description.InvocationKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLambdaArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose

object InvocationKindTransformer : FirTransformer<InvocationKind?>() {
    override fun <E : FirElement> transformElement(element: E, data: InvocationKind?): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformAnonymousFunction(
        anonymousFunction: FirAnonymousFunction,
        data: InvocationKind?
    ): CompositeTransformResult<FirDeclaration> {
        if (data != null) {
            anonymousFunction.replaceInvocationKind(data)
        }
        return anonymousFunction.compose()
    }

    override fun transformLambdaArgumentExpression(
        lambdaArgumentExpression: FirLambdaArgumentExpression,
        data: InvocationKind?
    ): CompositeTransformResult<FirStatement> {
        lambdaArgumentExpression.transformChildren(this, data)
        return lambdaArgumentExpression.compose()
    }

    override fun transformFunctionCall(functionCall: FirFunctionCall, data: InvocationKind?): CompositeTransformResult<FirStatement> {
        // TODO: add contracts handling
        return (functionCall.transformChildren(this, InvocationKind.EXACTLY_ONCE) as FirFunctionCall).compose()
    }
}