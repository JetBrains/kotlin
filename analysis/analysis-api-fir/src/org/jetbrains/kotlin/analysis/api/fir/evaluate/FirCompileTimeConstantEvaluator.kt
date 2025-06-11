/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.evaluate

import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.impl.base.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.evaluateAs
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.types.ConstantValueKind

/**
 * An evaluator that transform numeric operation, such as div, into compile-time constant iff involved operands, such as explicit receiver
 * and the argument, are compile-time constant as well.
 */
internal object FirCompileTimeConstantEvaluator {
    private val variablesInProcessOfEvaluation = ThreadLocal.withInitial { mutableSetOf<FirVariableSymbol<*>>() }

    /**
     * This mechanism is used to prevent issues cyclic dependencies between compile-time properties.
     * It ensures that each variable is not being evaluated again if it's already in the evaluation process.
     * ```
     *  // JavaConst.java
     *  public interface JavaConst {
     *     int x = KotlinConst.y;
     * }
     *
     * // KotlinConst.kt
     * object KotlinConst {
     *     const val y = JavaConst.x;
     * }
     * ```
     */
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

    fun evaluate(
        fir: FirElement?,
        firSession: FirSession
    ): FirLiteralExpression? = when (fir) {
        is FirPropertyAccessExpression -> {
            when (val referredVariable = fir.calleeReference.toResolvedVariableSymbol()) {
                is FirPropertySymbol -> referredVariable.evaluateRecursionAware(fir, firSession)
                is FirFieldSymbol -> referredVariable.evaluateRecursionAware(fir, firSession)
                else -> null
            }
        }
        is FirExpression -> fir.evaluateAs<FirLiteralExpression>(firSession)
        else -> null
    }


    fun evaluateAsKtConstantValue(
        fir: FirElement,
        firSession: FirSession
    ): KaConstantValue? {
        val evaluated = evaluate(fir, firSession) ?: return null

        val kind = evaluated.kind
        val value = kind.adjustType(evaluated.value)
        val psi = evaluated.psi as? KtElement
        return when (evaluated.kind) {
            ConstantValueKind.Byte -> KaByteConstantValueImpl(value as Byte, psi)
            ConstantValueKind.Int -> KaIntConstantValueImpl(value as Int, psi)
            ConstantValueKind.Long -> KaLongConstantValueImpl(value as Long, psi)
            ConstantValueKind.Short -> KaShortConstantValueImpl(value as Short, psi)

            ConstantValueKind.UnsignedByte -> KaUnsignedByteConstantValueImpl(value as UByte, psi)
            ConstantValueKind.UnsignedInt -> KaUnsignedIntConstantValueImpl(value as UInt, psi)
            ConstantValueKind.UnsignedLong -> KaUnsignedLongConstantValueImpl(value as ULong, psi)
            ConstantValueKind.UnsignedShort -> KaUnsignedShortConstantValueImpl(value as UShort, psi)

            ConstantValueKind.Double -> KaDoubleConstantValueImpl(value as Double, psi)
            ConstantValueKind.Float -> KaFloatConstantValueImpl(value as Float, psi)

            ConstantValueKind.Boolean -> KaBooleanConstantValueImpl(value as Boolean, psi)
            ConstantValueKind.Char -> KaCharConstantValueImpl(value as Char, psi)
            ConstantValueKind.String -> KaStringConstantValueImpl(value as String, psi)
            ConstantValueKind.Null -> KaNullConstantValueImpl(psi)


            ConstantValueKind.IntegerLiteral -> {
                val long = value as Long
                if (long in Int.MIN_VALUE..Int.MAX_VALUE) KaIntConstantValueImpl(long.toInt(), psi)
                else KaLongConstantValueImpl(long, psi)
            }

            ConstantValueKind.UnsignedIntegerLiteral -> {
                val long = value as ULong
                if (long in UInt.MIN_VALUE..UInt.MAX_VALUE) KaUnsignedIntConstantValueImpl(long.toUInt(), psi)
                else KaUnsignedLongConstantValueImpl(long, psi)
            }

            ConstantValueKind.Error -> errorWithFirSpecificEntries("Should not be possible to get from FIR tree", fir = fir)
        }
    }

    private fun FirVariableSymbol<*>.evaluateRecursionAware(
        expressionToEvaluate: FirExpression,
        firSession: FirSession
    ): FirLiteralExpression? =
        withTrackingVariableEvaluation(this) {
            expressionToEvaluate.evaluateAs<FirLiteralExpression>(firSession)
        }

    private fun ConstantValueKind.adjustType(value: Any?): Any? {
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
            ConstantValueKind.UnsignedByte -> {
                value as? UByte ?: (value as Number).toLong().toUByte()
            }
            ConstantValueKind.UnsignedShort -> {
                value as? UShort ?: (value as Number).toLong().toUShort()
            }
            ConstantValueKind.UnsignedInt -> {
                value as? UInt ?: (value as Number).toLong().toUInt()
            }
            ConstantValueKind.UnsignedLong -> {
                value as? ULong ?: (value as Number).toLong().toULong()
            }
            ConstantValueKind.UnsignedIntegerLiteral -> {
                when (value) {
                    is UInt -> value.toULong()
                    is ULong -> value
                    else -> (value as Number).toLong().toULong()
                }
            }
            ConstantValueKind.IntegerLiteral -> {
                when (value) {
                    is Int -> value.toLong()
                    is Long -> value
                    else -> (value as Number).toLong()
                }
            }
            else -> null
        }
    }
}
