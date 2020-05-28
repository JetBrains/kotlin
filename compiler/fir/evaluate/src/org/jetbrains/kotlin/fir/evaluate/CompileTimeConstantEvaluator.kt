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
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.valueParameters

/**
 * An evaluator that transform numeric operation, such as div, into compile-time constant iff involved operands, such as explicit receiver
 * and the argument, are compile-time constant as well.
 */
// TODO: Handle boolean operators and const property loading
class CompileTimeConstantEvaluator {

    fun evaluate(expression: FirExpression): FirConstExpression<*>? =
        if (expression is FirFunctionCall) {
            evaluate(expression)
        } else {
            null
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
            return typeRef.toFirConstKind() != null &&
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
        val expectedKind = expectedType.toFirConstKind()
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
        val number = typedValue
        return when (function.name.asString()) {
            "hashCode" ->
                buildConstExpression(source, FirConstKind.Int, number.hashCode())
            "dec" ->
                kind.toConstExpression(source, number.dec())
            "inc" ->
                kind.toConstExpression(source, number.inc())
            "unaryMinus" ->
                kind.toConstExpression(source, number.unaryMinus())
            "unaryPlus" ->
                kind.toConstExpression(source, number.unaryPlus())
            "toByte" ->
                buildConstExpression(source, FirConstKind.Byte, number.toByte())
            "toDouble" ->
                buildConstExpression(source, FirConstKind.Double, number.toDouble())
            "toFloat" ->
                buildConstExpression(source, FirConstKind.Float, number.toFloat())
            "toInt" ->
                buildConstExpression(source, FirConstKind.Int, number.toInt())
            "toLong" ->
                buildConstExpression(source, FirConstKind.Long, number.toLong())
            "toShort" ->
                buildConstExpression(source, FirConstKind.Short, number.toShort())
            else ->
                null
        }
    }

    // Binary operators
    private fun FirConstExpression<out Number>.evaluate(
        function: FirSimpleFunction,
        other: FirConstExpression<out Number>
    ): FirConstExpression<out Number>? {
        val n1 = typedValue
        val n2 = other.typedValue
        return when (function.name.asString()) {
            // TODO: more binary operators
            "plus" ->
                n1.plus(n2).let { v -> v.toFirConstKind()!!.toConstExpression(source, v) }
            else ->
                null
        }
    }

    ////// UNARY OPERATORS

    private fun Number.dec(): Number =
        when (this) {
            is Byte -> this.dec()
            is Double -> this.dec()
            is Float -> this.dec()
            is Int -> this.dec()
            is Long -> this.dec()
            is Short -> this.dec()
            else -> error("Unexpected Number kind: ${this.javaClass}")
        }

    private fun Number.inc(): Number =
        when (this) {
            is Byte -> this.inc()
            is Double -> this.inc()
            is Float -> this.inc()
            is Int -> this.inc()
            is Long -> this.inc()
            is Short -> this.inc()
            else -> error("Unexpected Number kind: ${this.javaClass}")
        }

    private fun Number.unaryMinus(): Number =
        when (this) {
            is Byte -> this.unaryMinus()
            is Double -> this.unaryMinus()
            is Float -> this.unaryMinus()
            is Int -> this.unaryMinus()
            is Long -> this.unaryMinus()
            is Short -> this.unaryMinus()
            else -> error("Unexpected Number kind: ${this.javaClass}")
        }

    private fun Number.unaryPlus(): Number =
        when (this) {
            is Byte -> this.unaryPlus()
            is Double -> this.unaryPlus()
            is Float -> this.unaryPlus()
            is Int -> this.unaryPlus()
            is Long -> this.unaryPlus()
            is Short -> this.unaryPlus()
            else -> error("Unexpected Number kind: ${this.javaClass}")
        }

    ////// BINARY OPERATORS

    // Design choice: reflection, which requires iteration over all declared functions every time, v.s., exhaustive type matching.
    private fun Number.binaryOperation(name: String, other: Number): Number? {
        val otherType = when (other) {
            is Byte -> Byte::class
            is Double -> Double::class
            is Float -> Float::class
            is Int -> Int::class
            is Long -> Long::class
            is Short -> Short::class
            else -> error("Unexpected Number kind: ${this.javaClass}")
        }
        return this::class.declaredFunctions.find { kFunction ->
            kFunction.name == name && kFunction.valueParameters.size == 1 && kFunction.valueParameters[0].type.classifier == otherType
        }?.call(other) as? Number
    }

    private fun Number.plus(other: Number): Number =
        when (this) {
            is Byte -> this.plus(other)
            is Double -> this.plus(other)
            is Float -> this.plus(other)
            is Int -> this.plus(other)
            is Long -> this.plus(other)
            is Short -> this.plus(other)
            else -> error("Unexpected Number kind: ${this.javaClass}")
        }

    private fun Byte.plus(other: Number): Number =
        when (other) {
            is Byte -> this.plus(other)
            is Double -> this.plus(other)
            is Float -> this.plus(other)
            is Int -> this.plus(other)
            is Long -> this.plus(other)
            is Short -> this.plus(other)
            else -> error("Unexpected Number kind: ${this.javaClass}")
        }

    private fun Double.plus(other: Number): Number =
        when (other) {
            is Byte -> this.plus(other)
            is Double -> this.plus(other)
            is Float -> this.plus(other)
            is Int -> this.plus(other)
            is Long -> this.plus(other)
            is Short -> this.plus(other)
            else -> error("Unexpected Number kind: ${this.javaClass}")
        }

    private fun Float.plus(other: Number): Number =
        when (other) {
            is Byte -> this.plus(other)
            is Double -> this.plus(other)
            is Float -> this.plus(other)
            is Int -> this.plus(other)
            is Long -> this.plus(other)
            is Short -> this.plus(other)
            else -> error("Unexpected Number kind: ${this.javaClass}")
        }

    private fun Int.plus(other: Number): Number =
        when (other) {
            is Byte -> this.plus(other)
            is Double -> this.plus(other)
            is Float -> this.plus(other)
            is Int -> this.plus(other)
            is Long -> this.plus(other)
            is Short -> this.plus(other)
            else -> error("Unexpected Number kind: ${this.javaClass}")
        }

    private fun Long.plus(other: Number): Number =
        when (other) {
            is Byte -> this.plus(other)
            is Double -> this.plus(other)
            is Float -> this.plus(other)
            is Int -> this.plus(other)
            is Long -> this.plus(other)
            is Short -> this.plus(other)
            else -> error("Unexpected Number kind: ${this.javaClass}")
        }

    private fun Short.plus(other: Number): Number =
        when (other) {
            is Byte -> this.plus(other)
            is Double -> this.plus(other)
            is Float -> this.plus(other)
            is Int -> this.plus(other)
            is Long -> this.plus(other)
            is Short -> this.plus(other)
            else -> error("Unexpected Number kind: ${this.javaClass}")
        }

    ////// KINDS

    private fun FirTypeRef.toFirConstKind(): FirConstKind<out Number>? =
        when (this) {
            !is FirResolvedTypeRef -> null
            !is FirImplicitBuiltinTypeRef -> type.toFirConstKind()
            is FirImplicitIntTypeRef -> FirConstKind.Int
            else -> null
        }

    private fun ConeKotlinType.toFirConstKind(): FirConstKind<out Number>? =
        when (this) {
            is ConeKotlinErrorType -> null
            is ConeLookupTagBasedType -> {
                when (lookupTag.name.asString()) {
                    "Byte" -> FirConstKind.Byte
                    "Double" -> FirConstKind.Double
                    "Float" -> FirConstKind.Float
                    "Int" -> FirConstKind.Int
                    "Long" -> FirConstKind.Long
                    "Short" -> FirConstKind.Short
                    else -> null
                }
            }
            is ConeFlexibleType -> upperBound.toFirConstKind()
            is ConeCapturedType -> lowerType?.toFirConstKind() ?: constructor.supertypes!!.first().toFirConstKind()
            is ConeDefinitelyNotNullType -> original.toFirConstKind()
            is ConeIntersectionType -> intersectedTypes.first().toFirConstKind()
            is ConeStubType -> null
            is ConeIntegerLiteralType -> null
        }

    private fun <T : Number> T.toFirConstKind(): FirConstKind<out Number>? =
        when (this) {
            is Byte -> FirConstKind.Byte
            is Double -> FirConstKind.Double
            is Float -> FirConstKind.Float
            is Int -> FirConstKind.Int
            is Long -> FirConstKind.Long
            is Short -> FirConstKind.Short
            else -> null
        }

    private fun FirConstKind<*>.convert(value: Number?): Number? {
        if (value == null) {
            return null
        }
        return when {
            this == FirConstKind.Byte -> value.toByte()
            this == FirConstKind.Double -> value.toDouble()
            this == FirConstKind.Float -> value.toFloat()
            this == FirConstKind.Int -> value.toInt()
            this == FirConstKind.Long -> value.toLong()
            this == FirConstKind.Short -> value.toShort()
            else -> null
        }
    }

    private fun <T : Number> FirConstKind<T>.toConstExpression(source: FirSourceElement?, value: Number): FirConstExpression<T> =
        @Suppress("UNCHECKED_CAST")
        buildConstExpression(source, this, value as T)

    // TODO: extension functions, e.g., Byte.and
    // TODO: toChar?, toString?
    companion object {
        private val unaryOperatorNames: Set<String> = setOf(
            "hashCode",
            "dec", "inc", "unaryMinus", "unaryPlus",
            "toByte", "toDouble", "toFloat", "toInt", "toLong", "toShort"
        )

        // TODO: rangeTo?
        private val binaryOperatorNames: Set<String> = setOf(
            "compareTo", "equals",
            "div", "minus", "mod", "plus", "rem", "times"
        )
    }
}

val <T> FirConstExpression<T>.typedValue: T
    @Suppress("UNCHECKED_CAST")
    get() =
        (value as? Long)?.let {
            when (kind) {
                FirConstKind.Byte -> it.toByte()
                FirConstKind.Short -> it.toShort()
                FirConstKind.Int -> it.toInt()
                FirConstKind.Float -> it.toFloat()
                FirConstKind.Double -> it.toDouble()
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
