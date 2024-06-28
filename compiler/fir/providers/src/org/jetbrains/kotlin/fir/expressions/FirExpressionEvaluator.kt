/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.contracts.description.LogicOperationKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.FirEvaluatorResult.*
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.evaluatedInitializer
import org.jetbrains.kotlin.fir.declarations.utils.isConst
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.constants.evaluate.CompileTimeType
import org.jetbrains.kotlin.resolve.constants.evaluate.evalBinaryOp
import org.jetbrains.kotlin.resolve.constants.evaluate.evalUnaryOp
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.util.OperatorNameConventions

@RequiresOptIn(
    "Internal FirExpressionEvaluator API. Should be avoided because it can be changed or dropped anytime. " +
            "Consider using `evaluatePropertyInitializer` or `evaluateAnnotationArguments` instead."
)
annotation class PrivateConstantEvaluatorAPI

object FirExpressionEvaluator {
    fun evaluatePropertyInitializer(property: FirProperty, session: FirSession): FirEvaluatorResult? {
        if (!property.isConst) {
            return null
        }

        val type = property.returnTypeRef.coneTypeOrNull?.fullyExpandedType(session)
        if (type == null || type is ConeErrorType || !type.canBeUsedForConstVal()) {
            return null
        }

        val initializer = property.initializer
        if (initializer == null || !initializer.canBeEvaluated(session)) {
            return null
        }

        return initializer.evaluate(session)
    }

    fun evaluateAnnotationArguments(annotation: FirAnnotation, session: FirSession): Map<Name, FirEvaluatorResult>? {
        val argumentMapping = annotation.argumentMapping.mapping

        if (argumentMapping.values.any { expr -> !expr.canBeEvaluated(session) }) {
            return null
        }

        return argumentMapping.mapValues { (_, expression) -> expression.evaluate(session) }
    }

    @PrivateConstantEvaluatorAPI
    fun evaluateExpression(expression: FirExpression, session: FirSession): FirEvaluatorResult? {
        if (!expression.canBeEvaluated(session)) return null
        return expression.evaluate(session)
    }

    private fun FirExpression?.canBeEvaluated(session: FirSession): Boolean {
        val intrinsicConstEvaluation = session.languageVersionSettings.supportsFeature(LanguageFeature.IntrinsicConstEvaluation)
        if (this == null || intrinsicConstEvaluation || this is FirLazyExpression || !isResolved) return false
        return canBeEvaluatedAtCompileTime(this, session, allowErrors = false, calledOnCheckerStage = false)
    }

    private fun FirExpression.evaluate(session: FirSession): FirEvaluatorResult {
        val visitor = EvaluationVisitor(session)
        return visitor.evaluate(this)
    }

    private fun <T> FirCallableSymbol<*>.visit(block: () -> T): T {
        val firProperty = this.fir as? FirProperty ?: return block()

        val oldEvaluatedResult = firProperty.evaluatedInitializer
        firProperty.evaluatedInitializer = DuringEvaluation
        try {
            return block()
        } finally {
            firProperty.evaluatedInitializer = oldEvaluatedResult
        }
    }

    private fun FirCallableSymbol<*>.wasVisited(): Boolean {
        val firProperty = this.fir as? FirProperty ?: return false
        return firProperty.evaluatedInitializer == DuringEvaluation
    }

    private class EvaluationVisitor(val session: FirSession) : FirVisitor<FirEvaluatorResult, Nothing?>() {
        fun evaluate(expression: FirExpression?): FirEvaluatorResult {
            return expression?.accept(this, null) ?: NotEvaluated
        }

        override fun visitElement(element: FirElement, data: Nothing?): FirEvaluatorResult {
            error("FIR element \"${element::class}\" is not supported in constant evaluation")
        }

        override fun visitLiteralExpression(literalExpression: FirLiteralExpression, data: Nothing?): FirEvaluatorResult {
            return literalExpression.wrap()
        }

        override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference, data: Nothing?): FirEvaluatorResult {
            return resolvedNamedReference.wrap()
        }

        override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: Nothing?): FirEvaluatorResult {
            return resolvedQualifier.wrap()
        }

        override fun visitGetClassCall(getClassCall: FirGetClassCall, data: Nothing?): FirEvaluatorResult {
            return getClassCall.wrap()
        }

        override fun visitArgumentList(argumentList: FirArgumentList, data: Nothing?): FirEvaluatorResult {
            return when (argumentList) {
                is FirResolvedArgumentList -> buildResolvedArgumentList(
                    argumentList.originalArgumentList,
                    argumentList.mapping.mapKeysTo(LinkedHashMap()) { evaluate(it.key).unwrapOr { return it } ?: return NotEvaluated },
                )
                else -> buildArgumentList {
                    source = argumentList.source
                    arguments.addAll(argumentList.arguments.map { evaluate(it).unwrapOr { return it } ?: return NotEvaluated })
                }
            }.wrap()
        }

        override fun visitNamedArgumentExpression(namedArgumentExpression: FirNamedArgumentExpression, data: Nothing?): FirEvaluatorResult {
            return buildNamedArgumentExpression {
                source = namedArgumentExpression.source
                annotations.addAll(namedArgumentExpression.annotations)
                expression = evaluate(namedArgumentExpression.expression).unwrapOr { return it } ?: return NotEvaluated
                isSpread = namedArgumentExpression.isSpread
                name = namedArgumentExpression.name
            }.wrap()
        }

        @OptIn(UnresolvedExpressionTypeAccess::class)
        override fun visitArrayLiteral(arrayLiteral: FirArrayLiteral, data: Nothing?): FirEvaluatorResult {
            return buildArrayLiteral {
                source = arrayLiteral.source
                coneTypeOrNull = arrayLiteral.coneTypeOrNull
                annotations.addAll(arrayLiteral.annotations)
                argumentList = visitArgumentList(arrayLiteral.argumentList, data).unwrapOr { return it } ?: return NotEvaluated
            }.wrap()
        }

        @OptIn(UnresolvedExpressionTypeAccess::class)
        override fun visitVarargArgumentsExpression(varargArgumentsExpression: FirVarargArgumentsExpression, data: Nothing?): FirEvaluatorResult {
            return buildVarargArgumentsExpression {
                source = varargArgumentsExpression.source
                coneTypeOrNull = varargArgumentsExpression.coneTypeOrNull
                annotations.addAll(varargArgumentsExpression.annotations)
                arguments.addAll(varargArgumentsExpression.arguments.map { evaluate(it).unwrapOr { return it } ?: return NotEvaluated })
                coneElementTypeOrNull = varargArgumentsExpression.coneElementTypeOrNull
            }.wrap()
        }

        override fun visitSpreadArgumentExpression(spreadArgumentExpression: FirSpreadArgumentExpression, data: Nothing?): FirEvaluatorResult {
            return buildSpreadArgumentExpression {
                source = spreadArgumentExpression.source
                annotations.addAll(spreadArgumentExpression.annotations)
                expression = evaluate(spreadArgumentExpression.expression).unwrapOr { return it } ?: return NotEvaluated
            }.wrap()
        }

        override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression, data: Nothing?): FirEvaluatorResult {
            val propertySymbol = propertyAccessExpression.calleeReference.toResolvedCallableSymbol()
                ?: return NotEvaluated

            if (propertySymbol.wasVisited()) {
                return RecursionInInitializer
            }

            fun evaluateOrCopy(initializer: FirExpression?): FirEvaluatorResult = propertySymbol.visit {
                if (initializer is FirLiteralExpression) {
                    // We need a copy here to copy a source of the original expression
                    initializer.copy(propertyAccessExpression).wrap()
                } else {
                    evaluate(initializer)
                }
            }

            return when (propertySymbol) {
                is FirPropertySymbol -> {
                    when {
                        propertySymbol.callableId.isStringLength || propertySymbol.callableId.isCharCode -> {
                            evaluate(propertyAccessExpression.explicitReceiver).let { receiver ->
                                val unaryArg = receiver.unwrapOr<FirExpression> { return it } ?: return NotEvaluated
                                evaluateUnary(unaryArg, propertySymbol.callableId)
                                    .adjustTypeAndConvertToLiteral(propertyAccessExpression)
                            }
                        }
                        else -> evaluateOrCopy(propertySymbol.fir.initializer)
                    }
                }
                is FirFieldSymbol -> evaluateOrCopy(propertySymbol.fir.initializer)
                is FirEnumEntrySymbol -> propertyAccessExpression.wrap()
                else -> error("FIR symbol \"${propertySymbol::class}\" is not supported in constant evaluation")
            }
        }

        override fun visitFunctionCall(functionCall: FirFunctionCall, data: Nothing?): FirEvaluatorResult {
            val calleeReference = functionCall.calleeReference
            if (calleeReference !is FirResolvedNamedReference) return NotEvaluated

            return when (val symbol = calleeReference.resolvedSymbol) {
                is FirNamedFunctionSymbol -> visitNamedFunction(functionCall, symbol)
                is FirConstructorSymbol -> visitConstructorCall(functionCall)
                else -> NotEvaluated
            }
        }

        private fun visitNamedFunction(functionCall: FirFunctionCall, symbol: FirNamedFunctionSymbol): FirEvaluatorResult {
            val receivers = listOfNotNull(functionCall.dispatchReceiver, functionCall.extensionReceiver)
            val evaluatedArgs = receivers.plus(functionCall.arguments).map {
                evaluate(it).unwrapOr<FirLiteralExpression> { return it } ?: return NotEvaluated
            }

            val opr1 = evaluatedArgs.getOrNull(0) ?: return NotEvaluated
            evaluateUnary(opr1, symbol.callableId)
                ?.adjustTypeAndConvertToLiteral(functionCall)
                ?.let { return it }

            val opr2 = evaluatedArgs.getOrNull(1) ?: return NotEvaluated
            evaluateBinary(opr1, symbol.callableId, opr2)
                ?.adjustTypeAndConvertToLiteral(functionCall)
                ?.let { return it }

            return NotEvaluated
        }

        @OptIn(UnresolvedExpressionTypeAccess::class)
        private fun visitConstructorCall(constructorCall: FirFunctionCall): FirEvaluatorResult {
            val type = constructorCall.resolvedType.fullyExpandedType(session).lowerBoundIfFlexible()
            when {
                type.toRegularClassSymbol(session)?.classKind == ClassKind.ANNOTATION_CLASS -> {
                    val evaluatedArgs = constructorCall.argumentList.accept(this, null)
                        .unwrapOr<FirResolvedArgumentList> { return it } ?: return NotEvaluated
                    return buildFunctionCall {
                        coneTypeOrNull = constructorCall.coneTypeOrNull
                        annotations.addAll(constructorCall.annotations)
                        typeArguments.addAll(constructorCall.typeArguments)
                        source = constructorCall.source
                        nonFatalDiagnostics.addAll(constructorCall.nonFatalDiagnostics)
                        argumentList = evaluatedArgs
                        calleeReference = constructorCall.calleeReference
                        origin = constructorCall.origin
                    }.wrap()
                }
                type.isUnsignedType -> {
                    val argument = evaluate(constructorCall.argument)
                        .unwrapOr<FirLiteralExpression> { return it }?.value ?: return NotEvaluated
                    return argument.adjustTypeAndConvertToLiteral(constructorCall)
                }
                else -> return NotEvaluated
            }
        }

        override fun visitIntegerLiteralOperatorCall(
            integerLiteralOperatorCall: FirIntegerLiteralOperatorCall,
            data: Nothing?
        ): FirEvaluatorResult {
            return visitFunctionCall(integerLiteralOperatorCall, data)
        }

        override fun visitComparisonExpression(comparisonExpression: FirComparisonExpression, data: Nothing?): FirEvaluatorResult {
            return visitFunctionCall(comparisonExpression.compareToCall, data).let {
                val intResult = it.unwrapOr<FirLiteralExpression> { return it }?.value as? Int ?: return NotEvaluated
                val compareToResult = when (comparisonExpression.operation) {
                    FirOperation.LT -> intResult < 0
                    FirOperation.LT_EQ -> intResult <= 0
                    FirOperation.GT -> intResult > 0
                    FirOperation.GT_EQ -> intResult >= 0
                    else -> error("Unsupported comparison operation type \"${comparisonExpression.operation.name}\"")
                }
                compareToResult.adjustTypeAndConvertToLiteral(comparisonExpression)
            }
        }

        override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: Nothing?): FirEvaluatorResult {
            val evaluatedArgs = equalityOperatorCall.arguments.map {
                evaluate(it).unwrapOr<FirLiteralExpression> { return it } ?: return NotEvaluated
            }
            if (evaluatedArgs.size != 2) return NotEvaluated

            val result = when (equalityOperatorCall.operation) {
                FirOperation.EQ -> evaluatedArgs[0].value == evaluatedArgs[1].value
                FirOperation.NOT_EQ -> evaluatedArgs[0].value != evaluatedArgs[1].value
                else -> error("Operation \"${equalityOperatorCall.operation}\" is not supported in compile time evaluation")
            }

            return result.toConstExpression(ConstantValueKind.Boolean, equalityOperatorCall).wrap()
        }

        override fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression, data: Nothing?): FirEvaluatorResult {
            val left = evaluate(binaryLogicExpression.leftOperand)
            val right = evaluate(binaryLogicExpression.rightOperand)

            val leftBoolean = left.unwrapOr<FirLiteralExpression> { return it }?.value as? Boolean ?: return NotEvaluated
            val rightBoolean = right.unwrapOr<FirLiteralExpression> { return it }?.value as? Boolean ?: return NotEvaluated
            val result = when (binaryLogicExpression.kind) {
                LogicOperationKind.AND -> leftBoolean && rightBoolean
                LogicOperationKind.OR -> leftBoolean || rightBoolean
                else -> error("Boolean logic expression of a kind \"${binaryLogicExpression.kind}\" is not supported in compile time evaluation")
            }

            return result.toConstExpression(ConstantValueKind.Boolean, binaryLogicExpression).wrap()
        }

        override fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall, data: Nothing?): FirEvaluatorResult {
            val strings = stringConcatenationCall.argumentList.arguments.map {
                evaluate(it).unwrapOr<FirLiteralExpression> { return it } ?: return NotEvaluated
            }
            val result = strings.joinToString(separator = "") { it.value.toString() }
            return result.toConstExpression(ConstantValueKind.String, stringConcatenationCall).wrap()
        }

        override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: Nothing?): FirEvaluatorResult {
            if (typeOperatorCall.operation != FirOperation.AS) return NotEvaluated
            val result = evaluate(typeOperatorCall.argument).unwrapOr<FirLiteralExpression> { return it } ?: return NotEvaluated
            if (result.resolvedType.isSubtypeOf(typeOperatorCall.resolvedType, session)) {
                return result.wrap()
            }
            return typeOperatorCall.wrap()
        }

        override fun visitEnumEntryDeserializedAccessExpression(
            enumEntryDeserializedAccessExpression: FirEnumEntryDeserializedAccessExpression,
            data: Nothing?,
        ): FirEvaluatorResult {
            return enumEntryDeserializedAccessExpression.wrap()
        }

        override fun visitClassReferenceExpression(
            classReferenceExpression: FirClassReferenceExpression,
            data: Nothing?,
        ): FirEvaluatorResult {
            return classReferenceExpression.wrap()
        }

        override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: Nothing?): FirEvaluatorResult {
            return visitAnnotation(annotationCall, data)
        }

        override fun visitAnnotation(annotation: FirAnnotation, data: Nothing?): FirEvaluatorResult {
            val mapping = annotation.argumentMapping.mapping
            if (mapping.isEmpty()) return annotation.wrap()
            val evaluatedMapping = mutableMapOf<Name, FirExpression>()
            for ((name, expression) in mapping) {
                when (val evaluatedExpression = evaluate(expression)) {
                    is Evaluated -> evaluatedMapping[name] = evaluatedExpression.result as FirExpression
                    else -> return evaluatedExpression
                }
            }
            return buildAnnotationCopy(annotation) {
                argumentMapping = buildAnnotationArgumentMapping {
                    this.mapping.putAll(evaluatedMapping)
                }
            }.wrap()
        }
    }
}

private fun ConstantValueKind.toCompileTimeType(): CompileTimeType {
    return when (this) {
        ConstantValueKind.Byte -> CompileTimeType.BYTE
        ConstantValueKind.Short -> CompileTimeType.SHORT
        ConstantValueKind.Int -> CompileTimeType.INT
        ConstantValueKind.Long -> CompileTimeType.LONG
        ConstantValueKind.Double -> CompileTimeType.DOUBLE
        ConstantValueKind.Float -> CompileTimeType.FLOAT
        ConstantValueKind.Char -> CompileTimeType.CHAR
        ConstantValueKind.Boolean -> CompileTimeType.BOOLEAN
        ConstantValueKind.String -> CompileTimeType.STRING

        else -> CompileTimeType.ANY
    }
}

// Unary operators
private fun evaluateUnary(arg: FirExpression, callableId: CallableId): Any? {
    if (arg !is FirLiteralExpression || arg.value == null) return null

    val opr = arg.kind.convertToGivenKind(arg.value) ?: return null
    return evalUnaryOp(
        callableId.callableName.asString(),
        arg.kind.toCompileTimeType(),
        opr
    )
}

// Binary operators
private fun evaluateBinary(
    arg1: FirExpression,
    callableId: CallableId,
    arg2: FirExpression
): Any? {
    if (arg1 !is FirLiteralExpression || arg1.value == null) return null
    if (arg2 !is FirLiteralExpression || arg2.value == null) return null
    // NB: some utils accept very general types, and due to the way operation map works, we should up-cast rhs type.
    val rightType = when {
        callableId.isStringEquals -> CompileTimeType.ANY
        callableId.isStringPlus -> CompileTimeType.ANY
        else -> arg2.kind.toCompileTimeType()
    }

    val opr1 = arg1.kind.convertToGivenKind(arg1.value) ?: return null
    val opr2 = arg2.kind.convertToGivenKind(arg2.value) ?: return null

    val functionName = callableId.callableName.asString()

    // Check for division by zero
    if (functionName == "div" || functionName == "rem") {
        if (rightType != CompileTimeType.FLOAT && rightType != CompileTimeType.DOUBLE && (opr2 as? Number)?.toInt() == 0) {
            // If expression is division by zero, then return the original expression as a result. We will handle on later steps.
            return DivisionByZero
        }
    }

    return evalBinaryOp(
        functionName,
        arg1.kind.toCompileTimeType(),
        opr1,
        rightType,
        opr2
    )
}

private fun Any?.adjustTypeAndConvertToLiteral(original: FirExpression): FirEvaluatorResult {
    if (this == null) return NotEvaluated
    if (this is FirEvaluatorResult) return this
    val expectedType = original.resolvedType
    val expectedKind = expectedType.toConstantValueKind() ?: return NotEvaluated
    val typeAdjustedValue = expectedKind.convertToGivenKind(this) ?: return NotEvaluated
    return typeAdjustedValue.toConstExpression(expectedKind, original).wrap()
}

private val CallableId.isStringLength: Boolean
    get() = classId == StandardClassIds.String && callableName.identifierOrNullIfSpecial == "length"

private val CallableId.isStringEquals: Boolean
    get() = classId == StandardClassIds.String && callableName == OperatorNameConventions.EQUALS

private val CallableId.isStringPlus: Boolean
    get() = classId == StandardClassIds.String && callableName == OperatorNameConventions.PLUS

private val CallableId.isCharCode: Boolean
    get() = packageName == StandardClassIds.BASE_KOTLIN_PACKAGE && classId == null && callableName.identifierOrNullIfSpecial == "code"

////// KINDS

private fun ConeKotlinType.toConstantValueKind(): ConstantValueKind? =
    when (this) {
        is ConeErrorType -> null
        is ConeLookupTagBasedType -> (lookupTag as? ConeClassLikeLookupTag)?.classId?.toConstantValueKind()
        is ConeFlexibleType -> upperBound.toConstantValueKind()
        is ConeCapturedType -> lowerType?.toConstantValueKind() ?: constructor.supertypes!!.first().toConstantValueKind()
        is ConeDefinitelyNotNullType -> original.toConstantValueKind()
        is ConeIntersectionType -> intersectedTypes.first().toConstantValueKind()
        is ConeStubType, is ConeIntegerLiteralType, is ConeTypeVariableType -> null
    }

private fun ClassId.toConstantValueKind(): ConstantValueKind? =
    when (this) {
        StandardClassIds.Byte -> ConstantValueKind.Byte
        StandardClassIds.Double -> ConstantValueKind.Double
        StandardClassIds.Float -> ConstantValueKind.Float
        StandardClassIds.Int -> ConstantValueKind.Int
        StandardClassIds.Long -> ConstantValueKind.Long
        StandardClassIds.Short -> ConstantValueKind.Short

        StandardClassIds.Char -> ConstantValueKind.Char
        StandardClassIds.String -> ConstantValueKind.String
        StandardClassIds.Boolean -> ConstantValueKind.Boolean

        StandardClassIds.UByte -> ConstantValueKind.UnsignedByte
        StandardClassIds.UShort -> ConstantValueKind.UnsignedShort
        StandardClassIds.UInt -> ConstantValueKind.UnsignedInt
        StandardClassIds.ULong -> ConstantValueKind.UnsignedLong

        else -> null
    }

private fun ConstantValueKind.convertToGivenKind(value: Any?): Any? {
    if (value == null) {
        return null
    }
    return when (this) {
        ConstantValueKind.Boolean -> value as Boolean
        ConstantValueKind.Char -> value as Char
        ConstantValueKind.String -> value as String
        ConstantValueKind.Byte -> (value as Number).toByte()
        ConstantValueKind.Double -> (value as Number).toDouble()
        ConstantValueKind.Float -> (value as Number).toFloat()
        ConstantValueKind.Int -> (value as Number).toInt()
        ConstantValueKind.Long -> (value as Number).toLong()
        ConstantValueKind.Short -> (value as Number).toShort()
        ConstantValueKind.UnsignedByte -> (value as Number).toLong().toUByte()
        ConstantValueKind.UnsignedShort -> (value as Number).toLong().toUShort()
        ConstantValueKind.UnsignedInt -> (value as Number).toLong().toUInt()
        ConstantValueKind.UnsignedLong -> (value as Number).toLong().toULong()
        ConstantValueKind.UnsignedIntegerLiteral -> (value as Number).toLong().toULong()
        else -> null
    }
}

private fun Any?.toConstExpression(
    kind: ConstantValueKind,
    originalExpression: FirExpression
): FirLiteralExpression {
    return buildLiteralExpression(
        originalExpression.source,
        kind,
        this,
        originalExpression.annotations.takeIf { it.isNotEmpty() }?.toMutableList(),
        setType = false,
    ).apply { replaceConeTypeOrNull(originalExpression.resolvedType) }
}

private fun FirLiteralExpression.copy(originalExpression: FirExpression): FirLiteralExpression {
    return this.value.toConstExpression(this.kind, originalExpression)
}

private fun FirElement?.wrap(): FirEvaluatorResult {
    return if (this != null) Evaluated(this) else NotEvaluated
}
