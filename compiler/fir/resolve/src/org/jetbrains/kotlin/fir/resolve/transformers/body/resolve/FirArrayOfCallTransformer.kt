/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildCollectionLiteral
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.isError
import org.jetbrains.kotlin.fir.resolve.isArrayOfCall
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.visitors.FirDefaultTransformer

/**
 * A transformer that converts resolved arrayOf() call to [FirCollectionLiteral].
 *
 * Note that arrayOf() calls only in [FirAnnotation] or the default value of annotation constructor are transformed.
 */
class FirArrayOfCallTransformer : FirDefaultTransformer<FirSession>() {
    private fun toArrayLiteral(functionCall: FirFunctionCall, session: FirSession): FirExpression? {
        if (!functionCall.isArrayOfCall(session)) return null
        if (functionCall.calleeReference !is FirResolvedNamedReference) return null
        val arrayLiteral = buildCollectionLiteral {
            source = functionCall.source
            annotations += functionCall.annotations
            // Note that the signature is: arrayOf(vararg element). Hence, unwrapping the original argument list here.
            argumentList = buildArgumentList {
                if (functionCall.arguments.isNotEmpty()) {
                    functionCall.arguments.flatMapTo(arguments) {
                        if (it is FirVarargArgumentsExpression) it.arguments else listOf(it)
                    }
                }
            }
            coneTypeOrNull = functionCall.resolvedType
        }

        val calleeReference = functionCall.calleeReference

        return if (calleeReference.isError()) {
            buildErrorExpression(
                functionCall.source?.fakeElement(KtFakeSourceElementKind.ErrorExpressionForTransformedArrayOf),
                calleeReference.diagnostic,
                arrayLiteral
            )
        } else {
            arrayLiteral
        }
    }

    override fun transformFunctionCall(functionCall: FirFunctionCall, data: FirSession): FirStatement {
        functionCall.transformChildren(this, data)
        return toArrayLiteral(functionCall, data) ?: functionCall
    }

    override fun <E : FirElement> transformElement(element: E, data: FirSession): E {
        @Suppress("UNCHECKED_CAST")
        return (element.transformChildren(this, data) as E)
    }

}

