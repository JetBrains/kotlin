/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.evaluate

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildConstExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitIntTypeRef
import org.jetbrains.kotlin.types.ConstantValueKind

/**
 * An evaluator that transform numeric operation, such as div, into compile-time constant iff involved operands, such as explicit receiver
 * and the argument, are compile-time constant as well.
 */
class FirCompileTimeConstantEvaluator {

    // TODO: Handle boolean operators, const property loading, class reference, array, annotation values, etc.
    fun evaluate(expression: FirExpression): FirConstExpression<*>? =
        when (expression) {
            is FirConstExpression<*> -> expression
            is FirFunctionCall -> evaluate(expression)
            else -> null
        }

    private fun evaluate(functionCall: FirFunctionCall): FirConstExpression<*>? {
        if (!functionCall.isNumericOperatorCall) {
            return null
        }
        val function = functionCall.getOriginalFunction()!! as FirSimpleFunction

        @Suppress("UNCHECKED_CAST")
        val opr1 = functionCall.explicitReceiver!! as FirConstExpression<out Number>
        if (function.name.asString() in unaryOperatorNames) {
            opr1.evaluate(function)?.let {
                return it.adjustType(functionCall.typeRef)
            }
            return null
        }
        assert(function.name.asString() in binaryOperatorNames) {
            "Inconsistency in isNumericOperatorCall and operator names: ${function.name.asString()}."
        }

        @Suppress("UNCHECKED_CAST")
        val opr2 = functionCall.argument as FirConstExpression<out Number>
        opr1.evaluate(function, opr2)?.let {
            return it.adjustType(functionCall.typeRef)
        }
        return null
    }

    private val FirFunctionCall.isNumericOperatorCall: Boolean
        get() {
            val function = getOriginalFunction() ?: return false
            return typeRef.toConstantValueKind() != null &&
                    // LHS is const expression with numeric constant value.
                    explicitReceiver != null && explicitReceiver!!.isNumericConst &&
                    function is FirSimpleFunction &&
                    // Either unary operator (which doesn't need RHS) or RHS is also const expression with numeric constant value.
                    ((function.name.asString() in unaryOperatorNames && arguments.isEmpty()) ||
                            (function.name.asString() in binaryOperatorNames && arguments.size == 1 && argument.isNumericConst))
        }

    private val FirExpression.isNumericConst: Boolean
        get() =
            this is FirConstExpression<*> && typedValue is Number

    private fun FirConstExpression<*>.adjustType(expectedType: FirTypeRef): FirConstExpression<*> {
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
                val typeAdjustedValue = expectedKind.convert(value as Number)!!
                expectedKind.toConstExpression(source, typeAdjustedValue)
            } else {
                this
            }
        // Lastly, we should preserve the resolved type of the original function call.
        return expression.apply {
            replaceTypeRef(expectedType)
        }
    }

    // Unary operators
    private fun FirConstExpression<out Number>.evaluate(function: FirSimpleFunction): FirConstExpression<out Number>? {
        val f = unaryOperations[UnaryOperationKey(kind, function.name.asString())] ?: return null
        // TODO: need some systematic check, e.g., integer overflow
        return try {
            val r = f(typedValue)
            r.toConstantValueKind()!!.toConstExpression(source, r)
        } catch (e: Exception) {
            null
        }
    }

    // Binary operators
    private fun FirConstExpression<out Number>.evaluate(
        function: FirSimpleFunction,
        other: FirConstExpression<out Number>
    ): FirConstExpression<out Number>? {
        val f = binaryOperations[BinaryOperationKey(kind, other.kind, function.name.asString())] ?: return null
        // TODO: need some systematic check, e.g., div by zero (1 / 0 v.s. 1 / 0.f)
        return try {
            val r = f(typedValue, other.typedValue)
            r.toConstantValueKind()!!.toConstExpression(source, r)
        } catch (e: Exception) {
            null
        }
    }

    ////// KINDS

    private fun FirTypeRef.toConstantValueKind(): ConstantValueKind<out Number>? =
        when (this) {
            !is FirResolvedTypeRef -> null
            !is FirImplicitBuiltinTypeRef -> type.toConstantValueKind()
            is FirImplicitIntTypeRef -> ConstantValueKind.Int
            else -> null
        }

    private fun ConeKotlinType.toConstantValueKind(): ConstantValueKind<out Number>? =
        when (this) {
            is ConeKotlinErrorType -> null
            is ConeLookupTagBasedType -> {
                when (lookupTag.name.asString()) {
                    "Byte" -> ConstantValueKind.Byte
                    "Double" -> ConstantValueKind.Double
                    "Float" -> ConstantValueKind.Float
                    "Int" -> ConstantValueKind.Int
                    "Long" -> ConstantValueKind.Long
                    "Short" -> ConstantValueKind.Short
                    else -> null
                }
            }
            is ConeFlexibleType -> upperBound.toConstantValueKind()
            is ConeCapturedType -> lowerType?.toConstantValueKind() ?: constructor.supertypes!!.first().toConstantValueKind()
            is ConeDefinitelyNotNullType -> original.toConstantValueKind()
            is ConeIntersectionType -> intersectedTypes.first().toConstantValueKind()
            is ConeStubType -> null
            is ConeIntegerLiteralType -> null
        }

    private fun <T : Number> T.toConstantValueKind(): ConstantValueKind<out Number>? =
        when (this) {
            is Byte -> ConstantValueKind.Byte
            is Double -> ConstantValueKind.Double
            is Float -> ConstantValueKind.Float
            is Int -> ConstantValueKind.Int
            is Long -> ConstantValueKind.Long
            is Short -> ConstantValueKind.Short
            else -> null
        }

    private fun ConstantValueKind<*>.convert(value: Number?): Number? {
        if (value == null) {
            return null
        }
        return when {
            this == ConstantValueKind.Byte -> value.toByte()
            this == ConstantValueKind.Double -> value.toDouble()
            this == ConstantValueKind.Float -> value.toFloat()
            this == ConstantValueKind.Int -> value.toInt()
            this == ConstantValueKind.Long -> value.toLong()
            this == ConstantValueKind.Short -> value.toShort()
            else -> null
        }
    }

    private fun <T : Number> ConstantValueKind<T>.toConstExpression(source: FirSourceElement?, value: Number): FirConstExpression<T> =
        @Suppress("UNCHECKED_CAST")
        buildConstExpression(source, this, value as T)
}

val <T> FirConstExpression<T>.typedValue: T
    @Suppress("UNCHECKED_CAST")
    get() =
        (value as? Long)?.let {
            when (kind) {
                ConstantValueKind.Byte -> it.toByte()
                ConstantValueKind.Short -> it.toShort()
                ConstantValueKind.Int -> it.toInt()
                ConstantValueKind.Float -> it.toFloat()
                ConstantValueKind.Double -> it.toDouble()
                else -> it
            }
        } as T ?: value

fun FirFunctionCall.getOriginalFunction(): FirCallableDeclaration<*>? {
    val symbol: AbstractFirBasedSymbol<*>? = when (val reference = calleeReference) {
        is FirResolvedNamedReference -> reference.resolvedSymbol
        // is FirNamedReferenceWithCandidate -> reference.candidateSymbol
        else -> null
    }
    return symbol?.fir as? FirCallableDeclaration<*>
}
