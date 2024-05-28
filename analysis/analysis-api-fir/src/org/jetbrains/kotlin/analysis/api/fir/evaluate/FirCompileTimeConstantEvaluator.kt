/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.evaluate

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.components.KaConstantEvaluationMode
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.isConst
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.constants.evaluate.CompileTimeType
import org.jetbrains.kotlin.resolve.constants.evaluate.evalBinaryOp
import org.jetbrains.kotlin.resolve.constants.evaluate.evalUnaryOp
import org.jetbrains.kotlin.types.ConstantValueKind

/**
 * An evaluator that transform numeric operation, such as div, into compile-time constant iff involved operands, such as explicit receiver
 * and the argument, are compile-time constant as well.
 */
internal object FirCompileTimeConstantEvaluator {
    private val variablesInProcessOfEvaluation = ThreadLocal.withInitial { mutableSetOf<FirVariableSymbol<*>>() }

    private inline fun <R> withTrackingVariableEvaluation(variableSymbol: FirVariableSymbol<*>, f: () -> R): R? {
        if (!variablesInProcessOfEvaluation.get().add(variableSymbol)) {
            return null
        }
        return try {
            f()
        } finally {
            variablesInProcessOfEvaluation.get().remove(variableSymbol)
        }
    }

    // TODO: Handle boolean operators, class reference, array, annotation values, etc.
    fun evaluate(
        fir: FirElement?,
        mode: KaConstantEvaluationMode,
    ): FirLiteralExpression? =
        when (fir) {
            is FirPropertyAccessExpression -> {
                when (val referredVariable = fir.calleeReference.toResolvedVariableSymbol()) {
                    is FirPropertySymbol -> {
                        if (referredVariable.callableId.isStringLength) {
                            evaluate(fir.explicitReceiver, mode)?.evaluateStringLength()
                        } else {
                            referredVariable.toLiteralExpression(mode)
                        }
                    }
                    is FirFieldSymbol -> referredVariable.toLiteralExpression(mode)
                    else -> null
                }
            }
            is FirLiteralExpression -> {
                fir.adaptToConstKind()
            }
            is FirFunctionCall -> {
                evaluateFunctionCall(fir, mode)
            }
            is FirStringConcatenationCall -> {
                evaluateStringConcatenationCall(fir, mode)
            }
            is FirNamedReference -> {
                fir.toResolvedPropertySymbol()?.toLiteralExpression(mode)
            }
            else -> null
        }

    private val CallableId.isStringLength: Boolean
        get() = classId == StandardClassIds.String && callableName.identifierOrNullIfSpecial == "length"

    private fun FirPropertySymbol.toLiteralExpression(
        mode: KaConstantEvaluationMode,
    ): FirLiteralExpression? {
        return when {
            mode == KaConstantEvaluationMode.CONSTANT_EXPRESSION_EVALUATION && !isConst -> null
            isVal -> {
                withTrackingVariableEvaluation(this) { evaluate(resolvedInitializer, mode) }
            }
            else -> null
        }
    }

    private fun FirFieldSymbol.toLiteralExpression(
        mode: KaConstantEvaluationMode,
    ): FirLiteralExpression? {
        return when {
            mode == KaConstantEvaluationMode.CONSTANT_EXPRESSION_EVALUATION && !(isStatic && isFinal) -> null
            isVal -> {
                withTrackingVariableEvaluation(this) { evaluate(resolvedInitializer, mode) }
            }
            else -> null
        }
    }

    fun evaluateAsKtConstantValue(
        fir: FirElement,
        mode: KaConstantEvaluationMode,
    ): KaConstantValue? {
        val evaluated = evaluate(fir, mode) ?: return null

        val value = evaluated.value
        val psi = evaluated.psi as? KtElement
        return when (evaluated.kind) {
            ConstantValueKind.Byte -> KaConstantValue.KaByteConstantValue(value as Byte, psi)
            ConstantValueKind.Int -> KaConstantValue.KaIntConstantValue(value as Int, psi)
            ConstantValueKind.Long -> KaConstantValue.KaLongConstantValue(value as Long, psi)
            ConstantValueKind.Short -> KaConstantValue.KaShortConstantValue(value as Short, psi)

            ConstantValueKind.UnsignedByte -> KaConstantValue.KaUnsignedByteConstantValue(value as UByte, psi)
            ConstantValueKind.UnsignedInt -> KaConstantValue.KaUnsignedIntConstantValue(value as UInt, psi)
            ConstantValueKind.UnsignedLong -> KaConstantValue.KaUnsignedLongConstantValue(value as ULong, psi)
            ConstantValueKind.UnsignedShort -> KaConstantValue.KaUnsignedShortConstantValue(value as UShort, psi)

            ConstantValueKind.Double -> KaConstantValue.KaDoubleConstantValue(value as Double, psi)
            ConstantValueKind.Float -> KaConstantValue.KaFloatConstantValue(value as Float, psi)

            ConstantValueKind.Boolean -> KaConstantValue.KaBooleanConstantValue(value as Boolean, psi)
            ConstantValueKind.Char -> KaConstantValue.KaCharConstantValue(value as Char, psi)
            ConstantValueKind.String -> KaConstantValue.KaStringConstantValue(value as String, psi)
            ConstantValueKind.Null -> KaConstantValue.KaNullConstantValue(psi)


            ConstantValueKind.IntegerLiteral -> {
                val long = value as Long
                if (Int.MIN_VALUE < long && long < Int.MAX_VALUE) KaConstantValue.KaIntConstantValue(long.toInt(), psi)
                else KaConstantValue.KaLongConstantValue(long, psi)
            }

            ConstantValueKind.UnsignedIntegerLiteral -> {
                val long = value as ULong
                if (UInt.MIN_VALUE < long && long < UInt.MAX_VALUE) KaConstantValue.KaUnsignedIntConstantValue(long.toUInt(), psi)
                else KaConstantValue.KaUnsignedLongConstantValue(long, psi)
            }

            ConstantValueKind.Error -> errorWithFirSpecificEntries("Should not be possible to get from FIR tree", fir = fir)
        }
    }

    private fun FirLiteralExpression.adaptToConstKind(): FirLiteralExpression {
        return kind.toLiteralExpression(
            source,
            kind.convertToNumber(value) ?: value
        )
    }

    private fun evaluateStringConcatenationCall(
        stringConcatenationCall: FirStringConcatenationCall,
        mode: KaConstantEvaluationMode,
    ): FirLiteralExpression? {
        val concatenated = buildString {
            for (arg in stringConcatenationCall.arguments) {
                val evaluated = evaluate(arg, mode) ?: return null
                append(evaluated.value.toString())
            }
        }

        return ConstantValueKind.String.toLiteralExpression(stringConcatenationCall.source, concatenated)
    }

    private fun evaluateFunctionCall(
        functionCall: FirFunctionCall,
        mode: KaConstantEvaluationMode,
    ): FirLiteralExpression? {
        val function = functionCall.getOriginalFunction() as? FirSimpleFunction ?: return null

        val opr1 = evaluate(functionCall.explicitReceiver, mode) ?: return null
        opr1.evaluate(function)?.let {
            return it.adjustType(functionCall.resolvedType)
        }

        val argument = functionCall.arguments.firstOrNull() ?: return null
        val opr2 = evaluate(argument, mode) ?: return null
        opr1.evaluate(function, opr2)?.let {
            return it.adjustType(functionCall.resolvedType)
        }
        return null
    }

    private fun FirLiteralExpression.adjustType(expectedType: ConeKotlinType): FirLiteralExpression {
        val expectedKind = expectedType.toConstantValueKind()
        // Note that the resolved type for the const expression is not always matched with the const kind. For example,
        //   fun foo(x: Int) {
        //     when (x) {
        //       -2_147_483_628 -> ...
        //   } }
        // That constant is encoded as `unaryMinus` call with the const 2147483628 of long type, while the resolved type is Int.
        // After computing the compile time constant, we need to adjust its type here.
        val expression =
            if (expectedKind != null && expectedKind != kind && value is Number) {
                val typeAdjustedValue = expectedKind.convertToNumber(value as Number)!!
                expectedKind.toLiteralExpression(source, typeAdjustedValue)
            } else {
                this
            }
        // Lastly, we should preserve the resolved type of the original function call.
        return expression.apply {
            replaceConeTypeOrNull(expectedType)
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
    private fun FirLiteralExpression.evaluate(function: FirSimpleFunction): FirLiteralExpression? {
        if (value == null) return null
        (value as? String)?.let { opr ->
            evalUnaryOp(
                function.name.asString(),
                kind.toCompileTimeType(),
                opr
            )?.let {
                return it.toConstantValueKind().toLiteralExpression(source, it)
            }
        }
        return kind.convertToNumber(value)?.let { opr ->
            evalUnaryOp(
                function.name.asString(),
                kind.toCompileTimeType(),
                opr
            )?.let {
                it.toConstantValueKind().toLiteralExpression(source, it)
            }
        }
    }

    private fun FirLiteralExpression.evaluateStringLength(): FirLiteralExpression? {
        return (value as? String)?.length?.let {
            it.toConstantValueKind().toLiteralExpression(source, it)
        }
    }

    // Binary operators
    private fun FirLiteralExpression.evaluate(
        function: FirSimpleFunction,
        other: FirLiteralExpression
    ): FirLiteralExpression? {
        if (value == null || other.value == null) return null
        // NB: some utils accept very general types, and due to the way operation map works, we should up-cast rhs type.
        val rightType = when {
            function.symbol.callableId.isStringEquals -> CompileTimeType.ANY
            function.symbol.callableId.isStringPlus -> CompileTimeType.ANY
            else -> other.kind.toCompileTimeType()
        }
        (value as? String)?.let { opr1 ->
            other.value?.let { opr2 ->
                evalBinaryOp(
                    function.name.asString(),
                    kind.toCompileTimeType(),
                    opr1,
                    rightType,
                    opr2
                )?.let {
                    return it.toConstantValueKind().toLiteralExpression(source, it)
                }
            }
        }
        return kind.convertToNumber(value)?.let { opr1 ->
            other.kind.convertToNumber(other.value)?.let { opr2 ->
                evalBinaryOp(
                    function.name.asString(),
                    kind.toCompileTimeType(),
                    opr1,
                    other.kind.toCompileTimeType(),
                    opr2
                )?.let {
                    it.toConstantValueKind().toLiteralExpression(source, it)
                }
            }
        }
    }

    private val CallableId.isStringEquals: Boolean
        get() = classId == StandardClassIds.String && callableName.identifierOrNullIfSpecial == "equals"

    private val CallableId.isStringPlus: Boolean
        get() = classId == StandardClassIds.String && callableName.identifierOrNullIfSpecial == "plus"

    ////// KINDS

    private fun ConeKotlinType.toConstantValueKind(): ConstantValueKind? =
        when (this) {
            is ConeErrorType -> null
            is ConeLookupTagBasedType -> lookupTag.name.asString().toConstantValueKind()
            is ConeFlexibleType -> upperBound.toConstantValueKind()
            is ConeCapturedType -> lowerType?.toConstantValueKind() ?: constructor.supertypes!!.first().toConstantValueKind()
            is ConeDefinitelyNotNullType -> original.toConstantValueKind()
            is ConeIntersectionType -> intersectedTypes.first().toConstantValueKind()
            is ConeStubType, is ConeIntegerLiteralType, is ConeTypeVariableType -> null
        }

    private fun String.toConstantValueKind(): ConstantValueKind? =
        when (this) {
            "Byte" -> ConstantValueKind.Byte
            "Double" -> ConstantValueKind.Double
            "Float" -> ConstantValueKind.Float
            "Int" -> ConstantValueKind.Int
            "Long" -> ConstantValueKind.Long
            "Short" -> ConstantValueKind.Short

            "Char" -> ConstantValueKind.Char
            "String" -> ConstantValueKind.String
            "Boolean" -> ConstantValueKind.Boolean

            else -> null
        }

    private fun <T> T.toConstantValueKind(): ConstantValueKind =
        when (this) {
            is Byte -> ConstantValueKind.Byte
            is Double -> ConstantValueKind.Double
            is Float -> ConstantValueKind.Float
            is Int -> ConstantValueKind.Int
            is Long -> ConstantValueKind.Long
            is Short -> ConstantValueKind.Short

            is Char -> ConstantValueKind.Char
            is String -> ConstantValueKind.String
            is Boolean -> ConstantValueKind.Boolean

            null -> ConstantValueKind.Null
            else -> error("Unknown constant value")
        }

    private fun ConstantValueKind.convertToNumber(value: Any?): Any? {
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

    private fun ConstantValueKind.toLiteralExpression(source: KtSourceElement?, value: Any?): FirLiteralExpression =
        buildLiteralExpression(source, this, value, setType = false)

    private fun FirFunctionCall.getOriginalFunction(): FirCallableDeclaration? {
        val symbol: FirBasedSymbol<*>? = when (val reference = calleeReference) {
            is FirResolvedNamedReference -> reference.resolvedSymbol
            else -> null
        }
        return symbol?.fir as? FirCallableDeclaration
    }
}
