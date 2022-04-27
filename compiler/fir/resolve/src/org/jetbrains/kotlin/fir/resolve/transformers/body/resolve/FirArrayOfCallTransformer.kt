/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildArrayOfCall
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.isArrayType
import org.jetbrains.kotlin.fir.visitors.FirDefaultTransformer
import org.jetbrains.kotlin.fir.visitors.FirTransformer

/**
 * A transformer that converts resolved arrayOf() call to [FirArrayOfCall].
 *
 * Note that arrayOf() calls only in [FirAnnotation] or the default value of annotation constructor are transformed.
 */
internal class FirArrayOfCallTransformer : FirDefaultTransformer<Nothing?>() {
    private val FirFunctionCall.isArrayOfCall: Boolean
        get() {
            val function: FirCallableDeclaration = getOriginalFunction() ?: return false
            return function is FirSimpleFunction &&
                    function.returnTypeRef.isArrayType &&
                    isArrayOf(function, arguments) &&
                    function.receiverTypeRef == null
        }

    private fun toArrayOfCall(functionCall: FirFunctionCall): FirArrayOfCall? {
        if (!functionCall.isArrayOfCall) {
            return null
        }
        return buildArrayOfCall {
            source = functionCall.source
            annotations += functionCall.annotations
            // Note that the signature is: arrayOf(vararg element). Hence, unwrapping the original argument list here.
            argumentList = buildArgumentList {
                if (functionCall.arguments.isNotEmpty()) {
                    (functionCall.argument as FirVarargArgumentsExpression).arguments.forEach {
                        arguments += it
                    }
                }
            }
            typeRef = functionCall.typeRef
        }
    }

    override fun transformFunctionCall(functionCall: FirFunctionCall, data: Nothing?): FirStatement {
        functionCall.transformChildren(this, data)
        return toArrayOfCall(functionCall) ?: functionCall
    }

    override fun <E : FirElement> transformElement(element: E, data: Nothing?): E {
        @Suppress("UNCHECKED_CAST")
        return (element.transformChildren(this, data) as E)
    }

    companion object {
        private val arrayOfNames = hashSetOf("kotlin/arrayOf") +
                hashSetOf(
                    "boolean", "byte", "char", "double", "float", "int", "long", "short",
                    "ubyte", "uint", "ulong", "ushort"
                ).map { "kotlin/" + it + "ArrayOf" }

        private fun isArrayOf(function: FirSimpleFunction, arguments: List<FirExpression>): Boolean =
            when (function.symbol.callableId.toString()) {
                "kotlin/emptyArray" -> function.valueParameters.isEmpty() && arguments.isEmpty()
                in arrayOfNames -> function.valueParameters.size == 1 && function.valueParameters[0].isVararg && arguments.size <= 1
                else -> false
            }
    }
}

private fun FirFunctionCall.getOriginalFunction(): FirCallableDeclaration? {
    val symbol: FirBasedSymbol<*>? = when (val reference = calleeReference) {
        is FirResolvedNamedReference -> reference.resolvedSymbol
        is FirNamedReferenceWithCandidate -> reference.candidateSymbol
        else -> null
    }
    return symbol?.fir as? FirCallableDeclaration
}
