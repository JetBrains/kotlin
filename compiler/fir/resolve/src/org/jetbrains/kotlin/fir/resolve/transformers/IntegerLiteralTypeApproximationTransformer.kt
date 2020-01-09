/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirResolvedNamedReferenceImpl
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.calls.ConeInferenceContext
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
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
    private val inferenceContext: ConeInferenceContext
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
        functionCall.resultType = data?.let { functionCall.resultType.resolvedTypeFromPrototype(it) } ?: resultSymbol.fir.returnTypeRef
        return functionCall.transformCalleeReference(StoreCalleeReference, FirResolvedNamedReferenceImpl(null, operator.name, resultSymbol!!)).compose()
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
            leftIsIlt -> rightArgument.typeRef.coneTypeUnsafe<ConeKotlinType>()
            rightIsIlt -> leftArgument.typeRef.coneTypeUnsafe<ConeKotlinType>()
            else -> throw IllegalStateException()
        }

        return operatorCall.transformArguments(this, expectedType).compose()
    }

    // TODO: call outside
    override fun transformTypeOperatorCall(
        typeOperatorCall: FirTypeOperatorCall,
        data: ConeKotlinType?
    ): CompositeTransformResult<FirStatement> {
        return typeOperatorCall.transformArguments(this, null).compose()
    }
}

fun ConeClassLikeType.toConstKind(): FirConstKind<*> {
    return when (classId) {
        StandardClassIds.Int -> FirConstKind.Int
        StandardClassIds.Long -> FirConstKind.Long
        StandardClassIds.Short -> FirConstKind.Short
        StandardClassIds.Byte -> FirConstKind.Byte
        else -> throw IllegalStateException()
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

class IntegerOperatorsTypeUpdater(val approximator: IntegerLiteralTypeApproximationTransformer) : FirTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformFunctionCall(functionCall: FirFunctionCall, data: Nothing?): CompositeTransformResult<FirStatement> {
        val function: FirCallableDeclaration<*> = functionCall.getOriginalFunction() ?: return functionCall.compose()

        if (function !is FirIntegerOperator) {
            val expectedType = function.receiverTypeRef?.coneTypeSafe<ConeKotlinType>()
            return functionCall.transformExplicitReceiver(approximator, expectedType).compose()
        }
        // TODO: maybe unsafe?
        val receiverValue = functionCall.explicitReceiver!!.typeRef.coneTypeSafe<ConeIntegerLiteralType>()?.value ?: return functionCall.compose()
        val kind = function.kind
        val resultValue = when {
            kind.unary -> when (kind) {
                FirIntegerOperator.Kind.UNARY_PLUS -> receiverValue
                FirIntegerOperator.Kind.UNARY_MINUS -> -receiverValue
                FirIntegerOperator.Kind.INV -> receiverValue.inv()
                else -> throw IllegalStateException()
            }
            else -> {
                val argumentType = functionCall.arguments.first().typeRef.coneTypeUnsafe<ConeKotlinType>()
                // TODO: handle overflow
                when (argumentType) {
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
        functionCall.replaceTypeRef(functionCall.resultType.resolvedTypeFromPrototype(ConeIntegerLiteralTypeImpl(resultValue)))
        return functionCall.toOperatorCall().compose()
    }
}

private fun FirFunctionCall.toOperatorCall(): FirIntegerOperatorCall {
    if (this is FirIntegerOperatorCall) return this
    return FirIntegerOperatorCall(source).also {
        it.typeRef = typeRef
        it.annotations += annotations
        it.safe = safe
        it.typeArguments += typeArguments
        it.explicitReceiver = explicitReceiver
        it.dispatchReceiver = dispatchReceiver
        it.extensionReceiver = extensionReceiver
        it.arguments += arguments
        it.calleeReference = calleeReference
    }
}