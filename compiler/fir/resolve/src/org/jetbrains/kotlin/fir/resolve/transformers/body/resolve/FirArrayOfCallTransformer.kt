/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildArrayLiteral
import org.jetbrains.kotlin.fir.references.FirResolvedErrorReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.isError
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.isArrayType
import org.jetbrains.kotlin.fir.visitors.FirDefaultTransformer

/**
 * A transformer that converts resolved arrayOf() call to [FirArrayLiteral].
 *
 * Note that arrayOf() calls only in [FirAnnotation] or the default value of annotation constructor are transformed.
 */
class FirArrayOfCallTransformer : FirDefaultTransformer<FirSession>() {
    private fun toArrayLiteral(functionCall: FirFunctionCall, session: FirSession): FirExpression? {
        if (!functionCall.isArrayOfCall(session)) return null
        if (functionCall.calleeReference !is FirResolvedNamedReference) return null
        val arrayLiteral = buildArrayLiteral {
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
            coneTypeOrNull = functionCall.coneTypeOrNull
        }

        val calleeReference = functionCall.calleeReference

        return if (calleeReference.isError()) {
            buildErrorExpression(
                functionCall.source?.fakeElement(KtFakeSourceElementKind.ErrorTypeRef),
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

    companion object {
        fun FirFunctionCall.isArrayOfCall(session: FirSession): Boolean {
            val function: FirCallableDeclaration = getOriginalFunction() ?: return false
            val returnTypeRef = function.returnTypeRef
            return function is FirSimpleFunction &&
                    returnTypeRef.coneTypeSafe<ConeKotlinType>()?.fullyExpandedType(session)?.isArrayType == true &&
                    isArrayOf(function, arguments) &&
                    function.receiverParameter == null
        }

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
        is FirResolvedErrorReference -> reference.resolvedSymbol
        is FirResolvedNamedReference -> reference.resolvedSymbol
        is FirNamedReferenceWithCandidate -> reference.candidateSymbol
        else -> null
    }
    return symbol?.fir as? FirCallableDeclaration
}
