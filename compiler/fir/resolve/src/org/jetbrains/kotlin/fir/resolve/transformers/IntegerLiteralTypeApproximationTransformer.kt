/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.propagateTypeFromOriginalReceiver
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.resolvedTypeFromPrototype
import org.jetbrains.kotlin.fir.scopes.impl.FirIntegerOperator
import org.jetbrains.kotlin.fir.scopes.impl.FirIntegerOperatorCall
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.types.AbstractTypeChecker

class IntegerLiteralTypeApproximationTransformer(
    private val symbolProvider: FirSymbolProvider,
    private val inferenceContext: ConeInferenceContext,
    private val session: FirSession
) : FirTransformer<ConeKotlinType?>() {
    override fun <E : FirElement> transformElement(element: E, data: ConeKotlinType?): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun <T> transformConstExpression(
        constExpression: FirConstExpression<T>,
        data: ConeKotlinType?
    ): CompositeTransformResult<FirStatement> {
        val type = constExpression.resultType.coneTypeSafe<ConeIntegerLiteralType>() ?: return constExpression.compose()
        val approximatedType = type.getApproximatedType(data)
        constExpression.resultType = constExpression.resultType.resolvedTypeFromPrototype(approximatedType)
        @Suppress("UNCHECKED_CAST")
        val kind = approximatedType.toConstKind() as FirConstKind<T>
        constExpression.replaceKind(kind)
        return constExpression.compose()
    }

    override fun transformFunctionCall(functionCall: FirFunctionCall, data: ConeKotlinType?): CompositeTransformResult<FirStatement> {
        val operator = functionCall.toResolvedCallableSymbol()?.fir as? FirIntegerOperator ?: return functionCall.compose()
        functionCall.transformChildren(this, data)
        val argumentType = functionCall.arguments.firstOrNull()?.resultType?.coneTypeUnsafe<ConeClassLikeType>()
        val receiverClassId = functionCall.dispatchReceiver.typeRef.coneTypeUnsafe<ConeClassLikeType>().lookupTag.classId
        val scope = declaredMemberScope((symbolProvider.getClassLikeSymbolByFqName(receiverClassId) as FirRegularClassSymbol).fir)
        var resultSymbol: FirFunctionSymbol<*>? = null
        scope.processFunctionsByName(operator.name) { symbol ->
            if (resultSymbol != null) {
                return@processFunctionsByName
            }
            if (operator.kind.unary) {
                resultSymbol = symbol
                return@processFunctionsByName
            }
            val function = symbol.fir
            val valueParameterType = function.valueParameters.first().returnTypeRef.coneTypeUnsafe<ConeClassLikeType>()
            if (AbstractTypeChecker.isSubtypeOf(inferenceContext, argumentType!!, valueParameterType)) {
                resultSymbol = symbol
                return@processFunctionsByName
            }
        }
        // TODO: Maybe resultType = data?
        //   check black box tests
        // e.g. Byte doesn't have `and` in member scope. It's an extension
        if (resultSymbol == null) return functionCall.compose()
        functionCall.resultType = data?.let { functionCall.resultType.resolvedTypeFromPrototype(it) } ?: resultSymbol!!.fir.returnTypeRef
        // If the original call has argument mapping, values in that mapping refer to value parameters in that original symbol. We should
        // map those original value parameters back to indices, and then renew the argument mapping with new value parameters in the result
        // symbol. Otherwise, while putting the value argument to the converted IR call, it will encounter an unknown value parameter,
        // resulting in an out-of-bound error.
        val newArgumentMapping =
            functionCall.argumentMapping?.mapValues { (_, oldValueParameter) ->
                val index = operator.valueParameters.indexOf(oldValueParameter)
                if (index != -1) resultSymbol!!.fir.valueParameters[index] else oldValueParameter
            }
        return functionCall.transformCalleeReference(
            StoreCalleeReference,
            buildResolvedNamedReference {
                name = operator.name
                resolvedSymbol = resultSymbol!!
            }
        ).apply {
            newArgumentMapping?.let {
                replaceArgumentList(buildResolvedArgumentList(it))
            }
        }.compose()
    }

    override fun transformOperatorCall(operatorCall: FirOperatorCall, data: ConeKotlinType?): CompositeTransformResult<FirStatement> {
        if (operatorCall.operation !in FirOperation.BOOLEANS) return operatorCall.compose()
        val leftArgument = operatorCall.arguments[0]
        val rightArgument = operatorCall.arguments[1]

        val leftIsIlt = leftArgument.typeRef.coneTypeSafe<ConeIntegerLiteralType>() != null
        val rightIsIlt = rightArgument.typeRef.coneTypeSafe<ConeIntegerLiteralType>() != null

        val expectedType: ConeKotlinType? = when {
            !leftIsIlt && !rightIsIlt -> return operatorCall.compose()
            leftIsIlt && rightIsIlt -> null
            leftIsIlt -> rightArgument.typeRef.coneType
            rightIsIlt -> leftArgument.typeRef.coneType
            else -> throw IllegalStateException()
        }

        operatorCall.argumentList.transformArguments(this, expectedType)
        return operatorCall.compose()
    }

    // TODO: call outside
    override fun transformTypeOperatorCall(
        typeOperatorCall: FirTypeOperatorCall,
        data: ConeKotlinType?
    ): CompositeTransformResult<FirStatement> {
        typeOperatorCall.argumentList.transformArguments(this, null)
        return typeOperatorCall.compose()
    }

    override fun transformCheckedSafeCallSubject(
        checkedSafeCallSubject: FirCheckedSafeCallSubject,
        data: ConeKotlinType?
    ): CompositeTransformResult<FirStatement> {
        val newReceiver =
            checkedSafeCallSubject.originalReceiverRef.value.transform<FirExpression, ConeKotlinType?>(this, data).single
        checkedSafeCallSubject.propagateTypeFromOriginalReceiver(newReceiver, session)
        return super.transformCheckedSafeCallSubject(checkedSafeCallSubject, data)
    }
}

fun FirFunctionCall.getOriginalFunction(): FirCallableDeclaration<*>? {
    val symbol: AbstractFirBasedSymbol<*>? = when (val reference = calleeReference) {
        is FirResolvedNamedReference -> reference.resolvedSymbol
        is FirNamedReferenceWithCandidate -> reference.candidateSymbol
        else -> null
    }
    return symbol?.fir as? FirCallableDeclaration<*>
}

class IntegerOperatorsTypeUpdater(private val approximator: IntegerLiteralTypeApproximationTransformer) : FirTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformFunctionCall(functionCall: FirFunctionCall, data: Nothing?): CompositeTransformResult<FirStatement> {
        val function: FirCallableDeclaration<*> = functionCall.getOriginalFunction() ?: return functionCall.compose()

        if (function !is FirIntegerOperator) {
            val expectedType = function.receiverTypeRef?.coneType
            return functionCall.transformExplicitReceiver(approximator, expectedType).compose()
        }
        // TODO: maybe unsafe?
        val receiverType = functionCall.explicitReceiver!!.typeRef.coneTypeSafe<ConeIntegerLiteralType>() ?: return functionCall.compose()
        val receiverValue = receiverType.value
        val kind = function.kind
        val resultValue = when {
            kind.unary -> when (kind) {
                FirIntegerOperator.Kind.UNARY_PLUS -> receiverValue
                FirIntegerOperator.Kind.UNARY_MINUS -> -receiverValue
                FirIntegerOperator.Kind.INV -> receiverValue.inv()
                else -> throw IllegalStateException()
            }
            else -> {
                // TODO: handle overflow
                when (val argumentType = functionCall.argument.typeRef.coneType) {
                    is ConeIntegerLiteralType -> {
                        val argumentValue = argumentType.value
                        val divisionByZero = argumentValue == 0L
                        when (kind) {
                            FirIntegerOperator.Kind.PLUS -> receiverValue + argumentValue
                            FirIntegerOperator.Kind.MINUS -> receiverValue - argumentValue
                            FirIntegerOperator.Kind.TIMES -> receiverValue * argumentValue
                            // TODO: maybe add some error reporting (e.g. in userdata)
                            FirIntegerOperator.Kind.DIV -> if (divisionByZero) receiverValue else receiverValue / argumentValue
                            FirIntegerOperator.Kind.REM -> if (divisionByZero) receiverValue else receiverValue % argumentValue
                            // TODO: check that argument can be int
                            FirIntegerOperator.Kind.SHL -> receiverValue shl argumentValue.toInt()
                            FirIntegerOperator.Kind.SHR -> receiverValue shr argumentValue.toInt()
                            FirIntegerOperator.Kind.USHR -> receiverValue ushr argumentValue.toInt()
                            FirIntegerOperator.Kind.XOR -> receiverValue xor argumentValue
                            FirIntegerOperator.Kind.AND -> receiverValue and argumentValue
                            FirIntegerOperator.Kind.OR -> receiverValue or argumentValue
                            else -> throw IllegalStateException()
                        }
                    }
                    else -> {
                        val expectedType = when (argumentType.classId) {
                            StandardClassIds.Long -> argumentType
                            else -> ConeIntegerLiteralTypeImpl.createType(StandardClassIds.Int)
                        }
                        functionCall.transformSingle(approximator, expectedType)
                        functionCall.replaceTypeRef(functionCall.resultType.resolvedTypeFromPrototype(expectedType))
                        return functionCall.compose()
                    }
                }
            }
        }
        val newTypeRef = functionCall.resultType.resolvedTypeFromPrototype(
            ConeIntegerLiteralTypeImpl(
                resultValue,
                isUnsigned = receiverType.isUnsigned
            )
        )
        functionCall.replaceTypeRef(newTypeRef)
        return functionCall.toOperatorCall().compose()
    }
}

@OptIn(FirImplementationDetail::class)
private fun FirFunctionCall.toOperatorCall(): FirIntegerOperatorCall {
    if (this is FirIntegerOperatorCall) return this
    return FirIntegerOperatorCall(
        source,
        typeRef,
        annotations.toMutableList(),
        typeArguments.toMutableList(),
        explicitReceiver,
        dispatchReceiver,
        extensionReceiver,
        argumentList,
        calleeReference,
    )
}
